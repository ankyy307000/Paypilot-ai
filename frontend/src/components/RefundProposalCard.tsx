import { useState } from 'react'

export type RefundAction = {
    id: string
    captureId: string
    amount: number
    currency: string
    status: string
    paypalRefundId: string | null
    createdAt?: string
}

type Props = {
    initialProposal: RefundAction
}

export default function RefundProposalCard({ initialProposal }: Props) {
    const [proposal, setProposal] = useState(initialProposal)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')

    async function handleAction(action: 'approve' | 'reject') {
        if (busy || proposal.status !== 'PENDING_APPROVAL') return

        if (action === 'approve') {
            const confirmed = window.confirm(
                `Execute a PayPal SANDBOX refund?\n\n` +
                `Capture: ${proposal.captureId}\n` +
                `Amount: ${proposal.amount} ${proposal.currency}\n\n` +
                `This will create a refund in PayPal Sandbox.`
            )
            if (!confirmed) return
        }

        setBusy(true)
        setError('')

        try {
            const response = await fetch(
                `/api/actions/${encodeURIComponent(proposal.id)}/${action}`,
                { method: 'POST' }
            )

            if (!response.ok) {
                throw new Error(`Request failed (${response.status})`)
            }

            const updated: RefundAction = await response.json()
            setProposal(updated)
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Request failed')

            if (action === 'approve') {
                // Do not offer another approval after an uncertain result.
                setProposal(current => ({
                    ...current,
                    status: 'PROCESSING'
                }))
            }
        } finally {
            setBusy(false)
        }
    }

    async function refreshStatus() {
        if (busy) return

        setBusy(true)
        setError('')

        try {
            const response = await fetch(
                `/api/actions/${encodeURIComponent(proposal.id)}`
            )

            if (!response.ok) {
                throw new Error(`Status check failed (${response.status})`)
            }

            const updated: RefundAction = await response.json()
            setProposal(updated)
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Status unavailable')
        } finally {
            setBusy(false)
        }
    }

    return (
        <div className="refund-proposal">
            <h3>Refund Proposal</h3>

            <p><strong>Capture:</strong> {proposal.captureId}</p>
            <p><strong>Amount:</strong> {proposal.amount} {proposal.currency}</p>
            <p><strong>Status:</strong> {proposal.status}</p>

            {proposal.paypalRefundId && (
                <p><strong>PayPal Refund ID:</strong> {proposal.paypalRefundId}</p>
            )}

            {proposal.status === 'PENDING_APPROVAL' && (
                <>
                    <p>Review the capture and amount before approving.</p>
                    <div className="refund-buttons">
                        <button
                            type="button"
                            disabled={busy}
                            onClick={() => handleAction('approve')}
                        >
                            {busy ? 'Processing...' : 'Approve Refund'}
                        </button>
                        <button
                            type="button"
                            disabled={busy}
                            className="reject-button"
                            onClick={() => handleAction('reject')}
                        >
                            Reject
                        </button>
                    </div>
                </>
            )}

            {proposal.status === 'PROCESSING' && (
                <p>
                    Refund is processing or its result is uncertain.
                    Verify the transaction in PayPal Sandbox before
                    taking further action.
                </p>
            )}

            {proposal.status === 'COMPLETED' && (
                <p>PayPal Sandbox refund completed.</p>
            )}

            {proposal.status === 'REJECTED' && (
                <p>Refund proposal rejected.</p>
            )}

            <button
                type="button"
                disabled={busy}
                onClick={refreshStatus}
            >
                Refresh Status
            </button>

            {error && <p className="error" role="alert">{error}</p>}
        </div>
    )
}
