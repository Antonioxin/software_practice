import { describe, expect, it } from 'vitest'
import {
  buildAuditQuery,
  buildTicketQuery,
  formatFenAmount,
  formatShanghai,
  historyActionLabel,
  localDatetimeToIso,
  shanghaiDateRange,
  shanghaiTodayRange,
  ticketStatusClass,
  ticketStatusLabel,
  ticketTypeLabel,
} from './presentation'

describe('support presentation helpers', () => {
  it('maps labels with fallback and builds badge classes', () => {
    expect(ticketTypeLabel('AFTER_SALES')).toBe('售后咨询')
    expect(ticketTypeLabel('UNKNOWN')).toBe('UNKNOWN')
    expect(ticketStatusLabel('REPLIED')).toBe('已回复')
    expect(ticketStatusLabel('WEIRD')).toBe('WEIRD')
    expect(historyActionLabel('FOLLOWED_UP')).toBe('用户补充')
    expect(historyActionLabel('SOMETHING')).toBe('SOMETHING')
    expect(ticketStatusClass('PROCESSING')).toBe('ticket-status--processing')
  })

  it('keeps meaningful ticket filters and drops blanks', () => {
    const query = buildTicketQuery({ type: '  PRODUCT ', status: 'NEW', from: '', to: '  ' }, 2, 12)
    expect(query.get('type')).toBe('PRODUCT')
    expect(query.get('status')).toBe('NEW')
    expect(query.has('from')).toBe(false)
    expect(query.has('to')).toBe(false)
    expect(query.get('page')).toBe('2')
    expect(query.get('pageSize')).toBe('12')
  })

  it('converts datetime-local inputs to ISO instants for from/to', () => {
    const query = buildTicketQuery(
      { type: '', status: '', from: '2026-09-01T08:00', to: '2026-09-06T10:30' },
    )
    expect(query.get('from')).toBe(new Date('2026-09-01T08:00').toISOString())
    expect(query.get('to')).toBe(new Date('2026-09-06T10:30').toISOString())
    expect(localDatetimeToIso('   ')).toBeNull()
    expect(localDatetimeToIso('not-a-date')).toBeNull()
  })

  it('keeps audit filters including actorId', () => {
    const query = buildAuditQuery(
      { actorId: '  1b2e6d9a-1111-2222-3333-444455556666 ', action: 'TICKET_CLOSED', objectType: '', from: '', to: '' },
      3,
    )
    expect(query.get('actorId')).toBe('1b2e6d9a-1111-2222-3333-444455556666')
    expect(query.get('action')).toBe('TICKET_CLOSED')
    expect(query.has('objectType')).toBe(false)
    expect(query.get('page')).toBe('3')
    expect(query.get('pageSize')).toBe('20')
  })

  it('formats integer-fen strings as CNY and rejects malformed input', () => {
    expect(formatFenAmount('123456')).toBe(`¥${(123456 / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`)
    expect(formatFenAmount('0')).toBe('¥0.00')
    expect(formatFenAmount('12.5')).toBe('—')
    expect(formatFenAmount('abc')).toBe('—')
    expect(formatFenAmount(null)).toBe('—')
    expect(formatFenAmount('')).toBe('—')
  })

  it('formats Shanghai dates around midnight boundary', () => {
    // 2026-09-06T03:30Z 是上海 09-06 11:30 → 当天从 09-05T16:00Z 开始
    expect(shanghaiTodayRange(new Date('2026-09-06T03:30:00Z')))
      .toEqual({ start: '2026-09-05T16:00:00.000Z', end: '2026-09-06T16:00:00.000Z' })
    // 2026-09-06T18:00Z 已是上海 09-07 凌晨 02:00 → 属于 09-07
    expect(shanghaiTodayRange(new Date('2026-09-06T18:00:00Z')))
      .toEqual({ start: '2026-09-06T16:00:00.000Z', end: '2026-09-07T16:00:00.000Z' })
    expect(shanghaiDateRange('2026-02-28'))
      .toEqual({ start: '2026-02-27T16:00:00.000Z', end: '2026-02-28T16:00:00.000Z' })
    expect(shanghaiDateRange('2026-9-6')).toBeNull()
    expect(shanghaiDateRange('garbage')).toBeNull()
  })

  it('renders Shanghai timezone text for valid instants only', () => {
    expect(formatShanghai('2026-09-06T16:00:00Z')).toContain('2026')
    expect(formatShanghai('')).toBe('—')
    expect(formatShanghai(null)).toBe('—')
    expect(formatShanghai('nope')).toBe('—')
  })
})
