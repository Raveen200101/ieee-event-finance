import type { ReactNode } from 'react'
import type { EventRecord } from '../types/event'

const navigation = [
  ['Chapter overview', 'overview', '#/chapter'],
  ['Events', 'events', '#/events'],
  ['Event overview', 'overview', 'event'],
  ['Planned budget', 'budget', ''],
  ['Purchase groups', 'purchases', ''],
  ['Itemized expenses', 'expenses', 'expenses'],
  ['Bank transfers', 'bank', ''],
  ['Fundraising', 'income', 'income'],
  ['Documents', 'documents', ''],
  ['Official Excel report', 'report', ''],
  ['Terms & members', 'members', ''],
  ['Team & invitations', 'members', ''],
]

function NavIcon({ kind }: { kind: string }) {
  const paths: Record<string, string> = {
    overview: 'M3 3h6v6H3z M15 3h6v6h-6z M3 15h6v6H3z M15 15h6v6h-6z',
    events: 'M4 5h16v16H4z M4 10h16 M8 2v6 M16 2v6',
    budget: 'M5 3h14v18H5z M8 7h8 M8 11h8 M8 15h5',
    purchases: 'M3 8h18v13H3z M7 8V4h10v4 M3 13h18',
    expenses: 'M5 3h14v18l-3-2-4 2-4-2-3 2z M8 8h8 M8 12h8',
    bank: 'M2 9l10-6 10 6z M4 21h16 M6 10v8 M12 10v8 M18 10v8',
    income: 'M12 2v20 M18 6H9a4 4 0 000 8h6a4 4 0 010 8H6',
    documents: 'M6 2h8l5 5v15H6z M14 2v6h5 M9 12h7 M9 16h7',
    report: 'M5 3h14v18H5z M8 8l8 8 M16 8l-8 8',
    members: 'M9 12a4 4 0 110-8 4 4 0 010 8 M2 22v-3a7 7 0 0114 0v3 M18 4a4 4 0 010 8 M19 16a5 5 0 013 4v2',
  }
  return <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" aria-hidden="true"><path d={paths[kind]} /></svg>
}

export function AppShell({ children, events, selected, page, term }: {
  children: ReactNode
  events: EventRecord[]
  selected: EventRecord | null
  page: string
  term: string
}) {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-eyebrow">IEEE / COMPUTER SOCIETY</div>
        <div className="brand-title">Chapter Finance</div>
        <p className="brand-subtitle">SLIIT Student Branch</p>
        <div className="term-card"><span>EXCOM TERM</span><strong>{selected?.term || term}</strong></div>
        <div className="nav-label">WORKSPACE</div>
        <nav aria-label="Chapter navigation">
          {navigation.map(([label, icon, destination]) => {
            let href = destination
            if (['event', 'expenses', 'income'].includes(destination)) {
              href = selected ? `#/events/${selected.id}${destination === 'event' ? '' : '/' + destination}` : ''
            }
            const active = destination === '#/events' ? page === 'events' || page === 'new'
              : destination === 'event' ? page === 'event'
              : destination === page
            return href && destination !== '#/chapter'
              ? <a key={label} href={href} aria-current={active ? 'page' : undefined}><NavIcon kind={icon} />{label}</a>
              : <button key={label} disabled title="Available in a later milestone"><NavIcon kind={icon} />{label}</button>
          })}
        </nav>
        <div className="security-context"><strong>INTERNAL · IEEE SLIIT</strong><p>Local development workspace.<br />Sign-in and role assignments are pending.</p></div>
      </aside>
      <div className="main-panel">
        <header className="topbar">
          <select aria-label="Event workspace" value={selected?.id || ''} onChange={(event) => { window.location.hash = event.target.value ? `/events/${event.target.value}` : '/events' }}>
            <option value="">Chapter workspace / {term}</option>
            {events.map((event) => <option key={event.id} value={event.id}>{event.name}</option>)}
          </select>
          <div className="role-context"><span>WORKSPACE</span><strong>Local development</strong></div>
        </header>
        <main className="workspace">{children}</main>
      </div>
    </div>
  )
}
