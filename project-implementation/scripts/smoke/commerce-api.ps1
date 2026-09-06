param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$Origin = 'http://localhost:5173',
    [Parameter(Mandatory)][string]$AdminEmail,
    [Parameter(Mandatory)][string]$AdminPassword,
    [string]$UserEmail = ('commerce-' + [guid]::NewGuid().ToString('N') + '@example.test'),
    [string]$UserPassword = 'CommerceSmokeTest123',
    [string]$ProductId = '20000000-0000-0000-0000-000000001002'
)
# PowerShell 7. Run only against an isolated test application/database.
# Creates one fictional adult test user and two orders; one is fulfilled, one refunded.
# Net inventory effect: one unit consumed. Never uses real payment/address data.
$ErrorActionPreference = 'Stop'
$userSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$adminSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
function Request($session, [string]$method, [string]$path, $body = $null, [string]$key = '') {
    $headers = @{ Origin = $Origin }
    if ($method -ne 'GET') {
        $csrf = (Invoke-RestMethod "$BaseUrl/api/v1/auth/csrf" -WebSession $session).data
        $headers[$csrf.headerName] = $csrf.token
    }
    if ($key) { $headers['Idempotency-Key'] = $key }
    $args = @{ Uri = "$BaseUrl/api/v1$path"; Method = $method; WebSession = $session; Headers = $headers; SkipHttpErrorCheck = $true; ContentType = 'application/json; charset=utf-8' }
    if ($null -ne $body) { $args.Body = ($body | ConvertTo-Json -Depth 10 -Compress); $args.ContentType = 'application/json; charset=utf-8' }
    $response = Invoke-WebRequest @args
    $content = if ($response.Content -is [byte[]]) { [Text.Encoding]::UTF8.GetString($response.Content) } else { $response.Content }
    if ($response.StatusCode -ge 400) { throw "$method $path => $($response.StatusCode): $content" }
    return @{ data = ($content | ConvertFrom-Json).data; status = $response.StatusCode; replayed = $response.Headers['Idempotency-Replayed'] -contains 'true' }
}
function Assert($condition, [string]$message) { if (-not $condition) { throw $message } }
$policy = (Request $userSession GET '/auth/registration-policy').data
$null = Request $userSession POST '/auth/register' @{
    email = $UserEmail; nickname = '交易测试账户'; password = $UserPassword; confirmPassword = $UserPassword
    adultConfirmed = $true; termsAccepted = $true; privacyAccepted = $true
    termsVersion = $policy.termsVersion; privacyVersion = $policy.privacyVersion
}
$null = Request $userSession POST '/auth/login' @{ email = $UserEmail; password = $UserPassword }
$null = Request $adminSession POST '/auth/login' @{ email = $AdminEmail; password = $AdminPassword }
function Create-TestOrder {
    $null = Request $userSession POST '/cart/items' @{ productId = $ProductId; quantity = 1 } ([guid]::NewGuid().ToString())
    $preview = (Request $userSession POST '/checkout-previews').data
    $key = [guid]::NewGuid().ToString()
    $body = @{ previewToken = $preview.previewToken; cartVersion = $preview.cartVersion; clientTotalFen = 100
        shippingAddress = @{ recipient = '模拟收件人'; phone = '13800000000'; countryOrRegion = '中国'; city = '上海'; addressLine = '模拟测试路123号' } }
    $created = Request $userSession POST '/orders' $body $key
    $replayed = Request $userSession POST '/orders' $body $key
    Assert ($created.status -eq 201 -and $replayed.status -eq 200 -and $replayed.replayed) '建单/重放HTTP状态不符合契约'
    Assert ($created.data.id -eq $replayed.data.id) '建单重放产生不同订单'
    Assert ($created.data.totalFen -eq $preview.totalFen) '服务端金额不等于可信预览'
    return $created.data
}
$first = Create-TestOrder
$failure = (Request $userSession POST "/orders/$($first.id)/mock-payments" @{ expectedVersion = $first.version; outcome = 'FAILURE' } ([guid]::NewGuid().ToString())).data
Assert ($failure.status -eq 'PENDING_PAYMENT') '模拟失败改变了订单状态'
$paid = (Request $userSession POST "/orders/$($first.id)/mock-payments" @{ expectedVersion = $failure.version; outcome = 'SUCCESS' } ([guid]::NewGuid().ToString())).data
$shipped = (Request $adminSession POST "/admin/orders/$($first.id)/mock-shipment" @{ expectedVersion = $paid.version; logisticsName = '模拟物流'; trackingNumber = 'SIM-SMOKE-123' } ([guid]::NewGuid().ToString())).data
$completed = (Request $userSession POST "/orders/$($first.id)/confirm-receipt" @{ expectedVersion = $shipped.version } ([guid]::NewGuid().ToString())).data
Assert ($completed.status -eq 'COMPLETED') '收货链路未完成'
$second = Create-TestOrder
$paid = (Request $userSession POST "/orders/$($second.id)/mock-payments" @{ expectedVersion = $second.version; outcome = 'SUCCESS' } ([guid]::NewGuid().ToString())).data
$key = [guid]::NewGuid().ToString(); $body = @{ expectedVersion = $paid.version; reason = '模拟取消测试' }
$cancelled = Request $userSession POST "/orders/$($second.id)/cancel" $body $key
$replayed = Request $userSession POST "/orders/$($second.id)/cancel" $body $key
Assert ($cancelled.data.status -eq 'CANCELLED' -and $cancelled.data.refunds.Count -eq 1 -and $replayed.replayed) '整单退款/重放不符合契约'
Assert ($cancelled.data.refunds[0].amountFen -eq $paid.totalFen) '退款额与付款额不符'
[ordered]@{ checkedAt = [DateTime]::UtcNow.ToString('o'); userEmail = $UserEmail; completedOrderId = $first.id; cancelledOrderId = $second.id; result = 'PASS'; mode = 'SIMULATED' } | ConvertTo-Json
