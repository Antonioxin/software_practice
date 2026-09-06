param(
    [string]$ApiBase = 'http://127.0.0.1:8080/api/v1',
    [Parameter(Mandatory = $true)][string]$AdminEmail,
    [Parameter(Mandatory = $true)][string]$AdminPassword
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Assert-That([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

$public = Invoke-RestMethod -Uri "$ApiBase/products?page=1&pageSize=50"
$publicItems = @($public.data)
Assert-That ($publicItems.Count -ge 14) '公开商品种子数量不足。'
$draftIds = @('20000000-0000-0000-0000-000000001015', '20000000-0000-0000-0000-000000001016')
Assert-That (@($publicItems | Where-Object { $_.id -in $draftIds }).Count -eq 0) '草稿泄漏到公开列表。'
Assert-That (-not ($publicItems[0].PSObject.Properties.Name -contains 'dealerReferenceUnitPriceFen')) '公开响应泄漏经销参考价。'

$unlisted = Invoke-RestMethod -Uri "$ApiBase/products/20000000-0000-0000-0000-000000001017"
Assert-That ($unlisted.data.status -eq 'UNLISTED' -and -not $unlisted.data.purchasable) '下架旧链接策略未生效。'

$session = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$csrf = Invoke-RestMethod -Uri "$ApiBase/auth/csrf" -WebSession $session
$headers = @{ Origin = 'http://localhost:5173'; $csrf.data.headerName = $csrf.data.token }
$loginBody = @{ email = $AdminEmail; password = $AdminPassword } | ConvertTo-Json
$actor = Invoke-RestMethod -Uri "$ApiBase/auth/login" -Method Post -WebSession $session `
    -Headers $headers -ContentType 'application/json' -Body $loginBody
Assert-That ($actor.data.baseRole -eq 'ADMIN') '管理员登录失败。'

$csrf = Invoke-RestMethod -Uri "$ApiBase/auth/csrf" -WebSession $session
$headers = @{ Origin = 'http://localhost:5173'; $csrf.data.headerName = $csrf.data.token }
$drafts = Invoke-RestMethod -Uri "$ApiBase/admin/products?status=DRAFT&page=1&pageSize=20" -WebSession $session
Assert-That (@($drafts.data).Count -ge 2) '后台未找到草稿种子。'

$targetResult = Invoke-RestMethod -Uri "$ApiBase/admin/products?keyword=WM-BOWL-003&page=1&pageSize=20" -WebSession $session
Assert-That (@($targetResult.data).Count -eq 1) '未找到库存冒烟目标商品。'
$target = @($targetResult.data)[0]
$before = $target.stock
$key = [guid]::NewGuid().ToString()
$adjustHeaders = $headers.Clone()
$adjustHeaders['Idempotency-Key'] = $key
$adjustBody = @{ direction = 'INCREASE'; quantity = 2; reason = '角色B接口冒烟补货' } | ConvertTo-Json

$first = Invoke-WebRequest -Uri "$ApiBase/admin/products/$($target.id)/stock-adjustments" `
    -Method Post -WebSession $session -Headers $adjustHeaders -ContentType 'application/json' -Body $adjustBody
$firstData = ($first.Content | ConvertFrom-Json).data
Assert-That ($firstData.stock -eq $before + 2) '库存增加结果不正确。'

$replay = Invoke-WebRequest -Uri "$ApiBase/admin/products/$($target.id)/stock-adjustments" `
    -Method Post -WebSession $session -Headers $adjustHeaders -ContentType 'application/json' -Body $adjustBody
$replayData = ($replay.Content | ConvertFrom-Json).data
Assert-That ($replayData.stock -eq $before + 2) '幂等重放重复增加了库存。'
Assert-That (@($replay.Headers['Idempotency-Replayed']) -contains 'true') '幂等重放响应头缺失。'

$publicAfter = Invoke-RestMethod -Uri "$ApiBase/products/$($target.id)"
Assert-That ($publicAfter.data.purchasable -and $publicAfter.data.stockStatus -eq 'IN_STOCK') '公开库存状态未及时刷新。'
$movements = Invoke-RestMethod -Uri "$ApiBase/admin/products/$($target.id)/stock-movements?page=1&pageSize=20" -WebSession $session
Assert-That ($movements.data[0].reason -eq '角色B接口冒烟补货') '库存流水原因未记录。'

Write-Output "CATALOG_API_SMOKE_OK published=$($public.meta.totalItems) drafts=$($drafts.meta.totalItems) stock=$($replayData.stock) movements=$($movements.meta.totalItems)"
