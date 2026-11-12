
import { useState } from 'react'

type RefundAction = {
    id: string
    captureId: string
    amount: number
    currency: string
    status: string
    paypalRefundId: string | null
}

type Mode = 'ai' | 'manual'

export default function RefundApproval() {
    const [mode, setMode] = useState<Mode>('ai')
    const [aiMessage, setAiMessage] = useState('')
    const [captureId, setCaptureId] = useState('')
    const [amount, setAmount] = useState('')
    const [proposal, setProposal] = useState<RefundAction | null>(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')

    async function readResponse(res: Response) {
        if (res.ok) return res.json()

        let details = ''
        try {
            const body = await res.json()
            details = body.detail || body.message || ''
        } catch {
            // Response may not contain JSON.
        }

        throw new Error(
            details || `Request failed (${res.status})`
        )
    }

    async function proposeRefund() {
        if (busy || proposal) return

        setBusy(true)
        setError('')

        try {
            const isAi = mode === 'ai'

            const endpoint = isAi
                ? '/api/agent/refund/propose'
                : '/api/actions/refund/propose'

            const payload = isAi
                ? { message: aiMessage.trim() }
                : {
                    captureId: captureId.trim(),
                    amount,
                    currency: 'USD'
                }

            if (isAi && !aiMessage.trim()) {
                throw new Error('Enter a refund request')
            }

            if (!isAi && (!captureId.trim() || !amount)) {
                throw new Error(
                    'Capture ID and amount are required'
                )
            }

            const res = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            })

            const data: RefundAction = await readResponse(res)
            setProposal(data)
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : 'Failed to create proposal'
            )
        } finally {
            setBusy(false)
        }
    }

    async function handleAction(
        action: 'approve' | 'reject'
    ) {
        if (!proposal || busy) return

        if (proposal.status !== 'PENDING_APPROVAL') {
            return
        }

        if (action === 'approve') {
            const confirmed = window.confirm(
                `Execute a PayPal SANDBOX refund?\n\n` +
                `Capture: ${proposal.captureId}\n` +
                `Amount: $${proposal.amount} ${proposal.currency}\n\n` +
                `This will create a refund in PayPal Sandbox.`
            )

            if (!confirmed) return
        }

        setBusy(true)
        setError('')

        try {
            const res = await fetch(
                `/api/actions/${encodeURIComponent(proposal.id)}/${action}`,
                { method: 'POST' }
            )

            const data: RefundAction = await readResponse(res)
            setProposal(data)
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : 'Action failed'
            )

            // After an uncertain approval result, prevent
            // another approval until the status is checked.
            if (action === 'approve') {
                setProposal({
                    ...proposal,
                    status: 'PROCESSING'
                })
            }
        } finally {
            setBusy(false)
        }
    }

    function resetForm() {
        if (busy) return

        setProposal(null)
        setError('')
        setAiMessage('')
        setCaptureId('')
        setAmount('')
    }

    return (
        <section className="panel">
            <h2>AI Refund Assistant</h2>
            <p>
                AI-powered financial actions ·
                Human approval required · PayPal Sandbox
            </p>

            <div className="refund-buttons">
                <button
                    type="button"
                    onClick={() => {
                        if (!proposal && !busy) setMode('ai')
                    }}
                    disabled={busy || !!proposal}
                    className={
                        mode === 'ai' ? 'active-mode' : ''
                    }
                >
                    AI Request
                </button>

                <button
                    type="button"
                    onClick={() => {
                        if (!proposal && !busy) setMode('manual')
                    }}
                    disabled={busy || !!proposal}
                    className={
                        mode === 'manual' ? 'active-mode' : ''
                    }
                >
                    Manual Refund
                </button>
            </div>

            {!proposal && (
                <>
                    {mode === 'ai' ? (
                        <>
                            <label htmlFor="ai-refund-request">
                                Describe your refund request
                            </label>

                            <textarea
                                id="ai-refund-request"
                                value={aiMessage}
                                onChange={e =>
                                    setAiMessage(e.target.value)
                                }
                                placeholder={
                                    'Refund 3 USD from transaction ' +
                                    '45614732KK142801G'
                                }
                                disabled={busy}
                                rows={3}
                            />
                        </>
                    ) : (
                        <>
                            <label htmlFor="refund-capture">
                                Capture ID
                            </label>
                            <input
                                id="refund-capture"
                                value={captureId}
                                onChange={e =>
                                    setCaptureId(e.target.value)
                                }
                                disabled={busy}
                                placeholder="PayPal Capture ID"
                            />

                            <label htmlFor="refund-amount">
                                Refund Amount (USD)
                            </label>
                            <input
                                id="refund-amount"
                                type="number"
                                min="0.01"
                                step="0.01"
                                value={amount}
                                onChange={e =>
                                    setAmount(e.target.value)
                                }
                                disabled={busy}
                            />
                        </>
                    )}

                    <button
                        type="button"
                        onClick={proposeRefund}
                        disabled={busy}
                    >
                        {busy
                            ? 'Processing...'
                            : mode === 'ai'
                                ? 'Generate AI Refund Proposal'
                                : 'Create Refund Proposal'}
                    </button>
                </>
            )}

            {proposal && (
                <div className="refund-proposal">
                    <h3>Refund Proposal</h3>

                    <p>
                        <strong>Capture:</strong>{' '}
                        {proposal.captureId}
                    </p>
                    <p>
                        <strong>Amount:</strong>{' '}
                        ${proposal.amount} {proposal.currency}
                    </p>
                    <p>
                        <strong>Status:</strong>{' '}
                        {proposal.status}
                    </p>

                    {proposal.paypalRefundId && (
                        <p>
                            <strong>PayPal Refund ID:</strong>{' '}
                            {proposal.paypalRefundId}
                        </p>
                    )}

                    {proposal.status ===
                        'PENDING_APPROVAL' && (
                            <>
                                <p>
                                    Review the transaction and amount
                                    before approving.
                                </p>

                                <div className="refund-buttons">
                                    <button
                                        type="button"
                                        onClick={() =>
                                            handleAction('approve')
                                        }
                                        disabled={busy}
                                    >
                                        Approve Refund
                                    </button>

                                    <button
                                        type="button"
                                        onClick={() =>
                                            handleAction('reject')
                                        }
                                        disabled={busy}
                                        className="reject-button"
                                    >
                                        Reject
                                    </button>
                                </div>
                            </>
                        )}

                    {proposal.status === 'PROCESSING' && (
                        <p>
                            Refund status is processing or
                            uncertain. Verify it in PayPal
                            Sandbox before taking further action.
                        </p>
                    )}

                    {proposal.status === 'COMPLETED' && (
                        <p>Refund completed successfully.</p>
                    )}

                    {proposal.status === 'REJECTED' && (
                        <p>Refund proposal rejected.</p>
                    )}

                    <button
                        type="button"
                        onClick={resetForm}
                        disabled={busy}
                    >
                        New Request
                    </button>
                </div>
            )}

            {error && (
                <p className="error" role="alert">
                    {error}
                </p>
            )}
        </section>
    )
}
