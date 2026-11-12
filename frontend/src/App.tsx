import { useEffect, useState } from 'react'
import ReactMarkdown from 'react-markdown'
import RefundApproval from './components/RefundApproval'
import RefundProposalCard from './components/RefundProposalCard'
import type { RefundAction } from './components/RefundProposalCard'
import RefundHistory from './components/RefundHistory'
import './App.css'

type Transaction = {
  id: string
  customer: string
  amount: number
  currency: string
  type: string
  status: string
  createdAt: string
}

type Summary = {
  totalRevenue: number
  totalTransactions: number
  totalRefunds: number
  netRevenue: number
  currency: string
  dataSource: string
}

type OrchestratorResponse = {
  intent: 'ANALYZE' | 'REFUND' | 'UNKNOWN'
  message: string
  refundAction: RefundAction | null
}

type ChatEntry = {
  id: number
  question: string
  response: OrchestratorResponse
}

function App() {
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [summary, setSummary] = useState<Summary | null>(null)
  const [dashboardError, setDashboardError] = useState('')

  const [message, setMessage] = useState('')
  const [chatHistory, setChatHistory] = useState<ChatEntry[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    async function loadDashboard() {
      try {
        const [transactionsResponse, summaryResponse] =
          await Promise.all([
            fetch('/api/transactions'),
            fetch('/api/transactions/summary')
          ])

        if (!transactionsResponse.ok || !summaryResponse.ok) {
          throw new Error('Unable to load financial data')
        }

        setTransactions(await transactionsResponse.json())
        setSummary(await summaryResponse.json())
      } catch (error) {
        setDashboardError(
          error instanceof Error ? error.message : 'Dashboard error'
        )
      }
    }

    loadDashboard()
  }, [])

  async function askPayPilot() {
    const question = message.trim()
    if (!question || loading) return

    setLoading(true)

    try {
      const response = await fetch('/api/agent/orchestrate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: question })
      })

      if (!response.ok) {
        let details = `AI request failed (${response.status})`

        try {
          const errorBody = await response.json()
          details = errorBody.detail || errorBody.message || details
        } catch {
          // Use the HTTP status if no JSON error is returned.
        }

        throw new Error(details)
      }

      const data: OrchestratorResponse = await response.json()

      setChatHistory(current => [
        ...current,
        {
          id: Date.now(),
          question,
          response: data
        }
      ])

      setMessage('')
    } catch (error) {
      setChatHistory(current => [
        ...current,
        {
          id: Date.now(),
          question,
          response: {
            intent: 'UNKNOWN',
            message: error instanceof Error
              ? error.message
              : 'Something went wrong',
            refundAction: null
          }
        }
      ])
    } finally {
      setLoading(false)
    }
  }

  function formatMoney(value: number, currency = 'USD') {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency
    }).format(value)
  }

  return (
    <main className="dashboard">
      <header className="dashboard-header">
        <div>
          <h1>PayPilot AI</h1>
          <p>Your Autonomous AI Finance Manager</p>
        </div>
        <span className="demo-badge">
          {summary?.dataSource ?? 'Loading'} DATA
        </span>
      </header>

      <h2>Financial Overview</h2>

      {dashboardError && (
        <p className="error">{dashboardError}</p>
      )}

      {summary ? (
        <div className="metrics">
          <div className="metric-card">
            <span>Total Revenue</span>
            <strong>{formatMoney(summary.totalRevenue)}</strong>
          </div>
          <div className="metric-card">
            <span>Transactions</span>
            <strong>{summary.totalTransactions}</strong>
          </div>
          <div className="metric-card">
            <span>Refunds</span>
            <strong>{formatMoney(summary.totalRefunds)}</strong>
          </div>
          <div className="metric-card">
            <span>Net Revenue</span>
            <strong>{formatMoney(summary.netRevenue)}</strong>
          </div>
        </div>
      ) : (
        <p>Loading financial summary...</p>
      )}

      <section className="panel">
        <h2>Recent Transactions</h2>

        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Transaction</th>
                <th>Customer</th>
                <th>Amount</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map(txn => (
                <tr key={txn.id}>
                  <td>{txn.id}</td>
                  <td>{txn.customer}</td>
                  <td>{formatMoney(txn.amount, txn.currency)}</td>
                  <td>
                    <span className="status">{txn.status}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <h2>Ask PayPilot AI</h2>
        <p>
          Analyze your finances or prepare a PayPal Sandbox
          refund through one AI assistant.
        </p>

        <textarea
          value={message}
          onChange={e => setMessage(e.target.value)}
          placeholder={
            'Analyze my recent transactions, or ' +
            'refund 1 USD from capture ABC123'
          }
          disabled={loading}
          rows={3}
        />

        <button
          type="button"
          onClick={askPayPilot}
          disabled={loading || !message.trim()}
        >
          {loading ? 'PayPilot is thinking...' : 'Ask PayPilot'}
        </button>

        {chatHistory.map(entry => (
          <div className="ai-response" key={entry.id}>
            <p><strong>You:</strong> {entry.question}</p>

            <h3>
              {entry.response.intent === 'ANALYZE'
                ? '📊 Finance Analyst Agent'
                : entry.response.intent === 'REFUND'
                  ? '💳 Refund Agent'
                  : '🤖 PayPilot Assistant'}
            </h3>

            <ReactMarkdown>
              {entry.response.message}
            </ReactMarkdown>

            {entry.response.refundAction && (
              <RefundProposalCard
                initialProposal={entry.response.refundAction}
              />
            )}
          </div>
        ))}
      </section>

      <RefundHistory />
      <RefundApproval />
    </main>
  )
}

export default App
