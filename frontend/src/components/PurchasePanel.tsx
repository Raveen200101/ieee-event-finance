import { useAuth } from './AuthContext'
import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { getBudget } from '../api/budgets'
import { getEventMembers, setPurchaseOwner } from '../api/assignments'
import type { EventMember } from '../api/assignments'
import { createPurchase, getPurchases, removeQuotation, saveQuotation, selectQuotation } from '../api/purchases'
import type { EventBudget } from '../types/budget'
import type { PurchaseGroup, Quotation } from '../types/purchase'

const money = (amount: number) => new Intl.NumberFormat('en-LK', { style: 'currency', currency: 'LKR', maximumFractionDigits: 2 }).format(amount)

export function PurchasePanel({ eventId }: { eventId: number }) {
  const { isChair, canManagePurchases } = useAuth()
  const [groups, setGroups] = useState<PurchaseGroup[]>()
  const [budget, setBudget] = useState<EventBudget>()
  const [members, setMembers] = useState<EventMember[]>([])
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)
  const [busy, setBusy] = useState(false)
  const [groupId, setGroupId] = useState<number>()
  const [quoteId, setQuoteId] = useState<number>()
  const [groupName, setGroupName] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [groupNotes, setGroupNotes] = useState('')
  const [supplier, setSupplier] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [unitPrice, setUnitPrice] = useState('')
  const [notes, setNotes] = useState('')

  useEffect(() => {
    let active = true
    Promise.all([getPurchases(eventId), isChair ? getBudget(eventId) : Promise.resolve(undefined), isChair ? getEventMembers(eventId) : Promise.resolve([])]).then(([purchases, allocations, team]) => {
      if (active) { setGroups(purchases); setBudget(allocations); setMembers(team); setError('') }
    }).catch((failure: Error) => { if (active) setError(failure.message) })
    return () => { active = false }
  }, [eventId, revision, isChair])

  async function mutate(action: () => Promise<PurchaseGroup[]>, message: string, after?: () => void) {
    setBusy(true); setError(''); setNotice('')
    try { setGroups(await action()); setNotice(message); after?.() }
    catch (failure) { setError(failure instanceof Error ? failure.message : 'Unable to save changes.') }
    finally { setBusy(false) }
  }
  function resetQuote() { setGroupId(undefined); setQuoteId(undefined); setSupplier(''); setQuantity('1'); setUnitPrice(''); setNotes('') }
  function startQuote(group: PurchaseGroup, quote?: Quotation) {
    setGroupId(group.id); setQuoteId(quote?.id); setSupplier(quote?.supplier || '')
    setQuantity(String(quote?.quantity || 1)); setUnitPrice(quote ? String(quote.unitPrice) : ''); setNotes(quote?.notes || '')
  }
  function addGroup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void mutate(() => createPurchase(eventId, { name: groupName.trim(), budgetCategoryId: Number(categoryId), notes: groupNotes.trim() }), 'Purchase group created.', () => { setGroupName(''); setCategoryId(''); setGroupNotes('') })
  }
  function submitQuote(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!groupId) return
    void mutate(() => saveQuotation(eventId, groupId, { supplier: supplier.trim(), quantity: Number(quantity), unitPrice: Number(unitPrice), notes: notes.trim() }, quoteId), quoteId ? 'Quotation updated. Select it again if it is still preferred.' : 'Quotation added.', resetQuote)
  }
  const allocated = budget?.categories.filter((row) => row.id !== null) || []
  const editingGroup = groups?.find((group) => group.id === groupId)
  const eligibleOwners = members.filter((member) => member.assigned && member.enabled && member.role === 'LOGISTICS_TEAM_LEAD')
  async function changeOwner(group: PurchaseGroup, value: string) {
    if (group.ownerId !== null && !window.confirm('Change this purchase-group assignment? The previous lead will lose access. Existing quotations will keep their original creator.')) return
    await mutate(async () => {
      await setPurchaseOwner(eventId, group.id, value ? Number(value) : undefined)
      return getPurchases(eventId)
    }, value ? 'Purchase-group assignment saved.' : 'Purchase-group assignment cleared.', resetQuote)
  }
  return <>
    <p className="info-strip">Compare supplier prices before purchasing. Selecting a quotation records your preference; record the actual expense separately when spending occurs.</p>
    {error && <div role="alert" className="error"><p>{error}</p><button className="secondary" disabled={busy} onClick={() => setRevision((value) => value + 1)}>Reload purchase groups</button></div>}
    {notice && <p role="status" className="info-strip">{notice}</p>}
    {!groups && !error && <p role="status">Loading purchase groups...</p>}
    {groups && <>
      {!groups.length && <section className="panel"><h2>No purchase groups yet</h2><p>Create a group for an item or service, then add supplier quotations to compare.</p></section>}
      {groups.map((group) => <section key={group.id} className="panel purchase-group" aria-label={group.name}>
        <div className="page-heading"><div><h2>{group.name}</h2><p>{group.category}{group.notes && ` · ${group.notes}`}</p></div>{canManagePurchases && <button disabled={busy} onClick={() => startQuote(group)}>+ Add quotation</button>}</div>
        {isChair && <div className="form-grid"><label>Logistics Team Lead<select aria-label={`Logistics Team Lead for ${group.name}`} disabled={busy} value={group.ownerId ?? ''} onChange={(event) => void changeOwner(group, event.target.value)}>
          <option value="">Unassigned — committee only</option>
          {group.ownerId !== null && !eligibleOwners.some((member) => member.id === group.ownerId) && <option value={group.ownerId} disabled>{members.find((member) => member.id === group.ownerId)?.displayName ?? `Member #${group.ownerId}`} (unavailable)</option>}
          {eligibleOwners.map((member) => <option key={member.id} value={member.id}>{member.displayName}</option>)}
        </select></label><p className="field-help">Only active Logistics Team Leads assigned in <a href={`#/events/${eventId}/members`}>Event team</a> are available. Reassignment preserves existing quotations and their creators.</p></div>}
        {isChair && <p>Category budget: <strong>{money(group.categoryBudget)}</strong> · Preferred quotations across this category: <strong>{money(group.categorySelectedTotal)}</strong></p>}
        {isChair && <p className={group.categoryRemaining < 0 ? 'error' : ''}>{group.categoryRemaining < 0 ? 'Selected costs exceed the category budget by ' : 'Category budget remaining after selected costs: '}{money(Math.abs(group.categoryRemaining))}</p>}
        <div className="table-scroll"><table className="quotation-table"><thead><tr><th>Supplier</th><th>Quantity</th><th>Unit price</th><th>Total</th><th>Notes</th><th>Decision</th>{canManagePurchases && <th>Actions</th>}</tr></thead><tbody>
          {group.quotations.map((quote) => <tr key={quote.id} className={quote.selected ? 'selected-quotation' : ''}>
            <td>{quote.supplier}</td><td>{quote.quantity.toLocaleString()}</td><td>{money(quote.unitPrice)}</td><td>{money(quote.total)}</td><td>{quote.notes || '—'}</td>
            <td>{quote.selected ? 'Preferred' : 'Not selected'}</td>{canManagePurchases && <td><div className="actions">
              {isChair && <button className="secondary" disabled={busy} onClick={() => void mutate(() => selectQuotation(eventId, group.id, quote.selected ? undefined : quote.id), quote.selected ? 'Selection cleared.' : 'Preferred quotation selected.')}>{quote.selected ? 'Clear selection' : 'Select'}</button>}
              <button className="secondary" disabled={busy || (!isChair && quote.selected)} onClick={() => startQuote(group, quote)}>Edit</button>
              <button className="secondary" disabled={busy || (!isChair && quote.selected)} aria-label={`Remove ${quote.supplier} quotation`} onClick={() => {
                if (window.confirm(`Remove the quotation from ${quote.supplier}?${quote.selected ? ' This also clears its selection.' : ''}`)) void mutate(() => removeQuotation(eventId, group.id, quote.id), 'Quotation removed.', () => { if (quoteId === quote.id) resetQuote() })
              }}>Remove</button>
            </div></td>}
          </tr>)}
        </tbody></table></div>
        {!group.quotations.length && <p className="empty-state">No quotations yet. Add supplier, quantity and unit price to start comparing.</p>}
        {canManagePurchases && editingGroup?.id === group.id && <form className="quotation-form" onSubmit={submitQuote}>
          <h3>{quoteId ? 'Edit quotation' : 'Add quotation'}</h3>
          <fieldset disabled={busy}><div className="form-grid">
            <label>Supplier<input required maxLength={150} value={supplier} onChange={(event) => setSupplier(event.target.value)} /></label>
            <label>Quantity<input type="number" required min="1" max="1000000" step="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} /></label>
            <label>Unit price (LKR)<input type="number" required min="0.01" max="9999999999999.99" step="0.01" value={unitPrice} onChange={(event) => setUnitPrice(event.target.value)} /></label>
            <label>Quotation notes<textarea maxLength={1000} value={notes} onChange={(event) => setNotes(event.target.value)} /></label>
          </div><p>Total: <strong>{money(Number(quantity) * Number(unitPrice))}</strong></p>
            <p className="field-help">Enter the complete unit price including applicable charges. Attachments and approval roles will be added later. Editing a quotation clears its preferred status.</p>
            <div className="actions form-actions"><button type="submit">{busy ? 'Saving...' : 'Save quotation'}</button><button type="button" className="secondary" onClick={resetQuote}>Cancel</button></div>
          </fieldset>
        </form>}
      </section>)}
      {isChair && budget && <section className="panel budget-form"><h2>Create purchase group</h2>
        {!allocated.length ? <p>Add a category in <a href={`#/events/${eventId}/budget`}>Planned budget</a> before creating a purchase group.</p> :
          <form onSubmit={addGroup}><fieldset disabled={busy}><div className="form-grid">
            <label>Group name<input required maxLength={150} value={groupName} onChange={(event) => setGroupName(event.target.value)} placeholder="Event T-shirts" /></label>
            <label>Budget category<select aria-label="Budget category" required value={categoryId} onChange={(event) => setCategoryId(event.target.value)}><option value="">Select a category</option>{allocated.map((row) => <option key={row.id} value={row.id!}>{row.category}</option>)}</select></label>
            <label className="full-width">Group notes<textarea maxLength={1000} value={groupNotes} onChange={(event) => setGroupNotes(event.target.value)} /></label>
          </div><div className="actions form-actions"><button type="submit">{busy ? 'Saving...' : 'Create purchase group'}</button></div></fieldset></form>}
      </section>}
    </>}
  </>
}
