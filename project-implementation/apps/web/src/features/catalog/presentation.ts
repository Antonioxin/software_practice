export function formatCny(fen?: number | null): string {
  return fen == null ? '—' : `¥${(fen / 100).toFixed(2)}`
}

/** Keep older order and inquiry snapshots readable after the catalog names are cleaned up. */
export function formatProductName(name: string): string {
  return name.replace(/\s*(?:·\s*)?示例\s*$/u, '') || name
}

export function formatAgeRange(ageMin: number, ageMax?: number | null): string {
  return ageMax == null ? `${ageMin} 岁以上` : `${ageMin}—${ageMax} 岁`
}

export function buildCatalogQuery(
  filters: Record<string, string>,
  page = 1,
  pageSize = 12,
): URLSearchParams {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    const normalized = value.trim()
    if (normalized) params.set(key, normalized)
  }
  params.set('page', String(page))
  params.set('pageSize', String(pageSize))
  return params
}
