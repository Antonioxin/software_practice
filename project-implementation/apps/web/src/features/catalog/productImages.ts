// Name-based AI illustrations, not product photography or a media API response.
// Keep this explicit SKU allowlist independent of development preview fixtures.
const illustrations = new Map<string, string>([
  ['WM-BALANCE-STONES', '/assets/products/balance-stones.png'],
  ['WM-RAINBOW-ARCH', '/assets/products/rainbow-arch.png'],
  ['WM-RING-TOSS', '/assets/products/ring-toss.png'],
  ['WM-TEAM-BOARD', '/assets/products/team-board.png'],
  ['WM-FOREST-KIT', '/assets/products/forest-kit.png'],
  ['WM-SKIP-ROPE', '/assets/products/skip-rope.png'],
])

export function productIllustrationSrc(sku: string): string | null {
  return illustrations.get(sku) ?? null
}

export function isManagedImage(id?: string | null): boolean {
  return !!id && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id)
}

export function productImageSrc(sku: string, mainImageId?: string | null): string | null {
  return isManagedImage(mainImageId) ? `/api/v1/media/${mainImageId}/content` : productIllustrationSrc(sku)
}
