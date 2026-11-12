import { useCallback, useEffect, useState } from 'react'
import type { RefundAction } from './RefundProposalCard'

export default function RefundHistory() {
    const [actions, setActions] = useState<RefundAction[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    const loadHistory = useCallback(async () => {
        setLoading(true)
        setError('')

        try {
            const response = await fetch('/api/actions/history')

            if (!response.ok) {
                throw new Error(`History request failed (${response.status})`)
            }

            const data: RefundAction[] = await response.json()
            setActions(data)
        } catch (err) {
            setError(
                err instanceof Error ? err.message : 'Unable to load history'
            )
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        const controller = new AbortController()

        async function fetchHistory() {
            try {
                const response = await fetch('/api/actions/history', {
                    signal: controller.signal
                })

                if (!response.ok) {
                    throw new Error(`History request failed (${response.status})`)
                }

                const data: RefundAction[] = await response.json()

                if (!controller.signal.aborted) {
                    setActions(data)
                }
            } catch (err) {
                if (!controller.signal.aborted) {
                    setError(
                        err instanceof Error ? err.message : 'Unable to load history'
                    )
                }
            } finally {
                if (!controller.signal.aborted) {
                    setLoading(false)
                }
            }
        }

        void fetchHistory()

        return () => controller.abort()
    }, [])

    return (
        <section className="panel">
            <div className="history-header">
                <div>
                    <h2>Refund History & Audit Trail</h2>
                    <p>Refund actions recorded by PayPilot in H2.</p>
                </div>

                <button
                    type="button"
                    disabled={loading}
                    onClick={() => void loadHistory()}
                >
                    {loading ? 'Loading...' : 'Refresh History'}
                </button>
            </div>

            {error && <p className="error" role="alert">{error}</p>}

            {!loading && !error && actions.length === 0 && (
                <p>No refund actions recorded yet.</p>
            )}

            {actions.length > 0 && (
                <div className="table-scroll">
                    <table>
                        <thead>
                            <tr>
                                <th>Capture ID</th>
                                <th>Amount</th>
                                <th>Status</th>
                                <th>Created</th>
                                <th>PayPal Refund ID</th>
                            </tr>
                        </thead>

                        <tbody>
                            {actions.map(action => (
                                <tr key={action.id}>
                                    <td>{action.captureId}</td>
                                    <td>
                                        {action.amount} {action.currency}
                                    </td>
                                    <td>
                                        <span className={`status status-${action.status.toLowerCase()}`}>
                                            {action.status.replaceAll('_', ' ')}
                                        </span>
                                    </td>
                                    <td>
                                        {action.createdAt
                                            ? new Date(action.createdAt).toLocaleString()
                                            : '—'}
                                    </td>
                                    <td>{action.paypalRefundId || '—'}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <p className="history-note">
                This ledger tracks PayPilot-managed actions only.
                Refunds made outside PayPilot may not appear.
            </p>
        </section>
    )
}
