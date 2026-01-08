import React, { useState, useEffect } from 'react'
import axios from 'axios'
import Markdown from 'react-markdown'

interface Document {
  id: string
  title: string
  status: string
}

interface Chat {
  id: string
  title: string
}

interface Message {
  role: string
  content: string
  sources?: any[]
}

const App = () => {
  const [documents, setDocuments] = useState<Document[]>([])
  const [chats, setChats] = useState<Chat[]>([])
  const [currentChatId, setCurrentChatId] = useState<string | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [uploading, setUploading] = useState(false)

  const exampleQuestions = [
    "Shipments from Acme Industrial to Stark vs Wayne",
    "Steel Bracket (ITEM-1001) summary",
    "Carrier XPO Logistics summary",
    "Quality inspections for ITEM-1001",
    "Invoices December 2023",
    "Handling notes for ITEM-7712",
    "Acme Industrial & Stark Components relationship",
    "Corrective actions for quality inspections",
    "Discrepancies for Hydraulic Hose (ITEM-1044)",
    "Bank details for Acme Industrial"
  ];

  // Set Default Header for Mock Auth
  axios.defaults.headers.common['X-User-Email'] = 'demo@example.com';

  const fetchDocs = async () => {
    try {
      const res = await axios.get('/api/docs')
      setDocuments(res.data)
    } catch (e) { console.error(e) }
  }

  const fetchChats = async () => {
    try {
      const res = await axios.get('/api/chats')
      setChats(res.data)
    } catch (e) { console.error(e) }
  }

  useEffect(() => {
    fetchDocs()
    fetchChats()
  }, [])

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || e.target.files.length === 0) return
    setUploading(true)
    const formData = new FormData()
    formData.append('file', e.target.files[0])
    try {
      await axios.post('/api/docs', formData)
      fetchDocs()
    } catch (err) {
      alert('Upload failed')
      console.error(err)
    } finally {
      setUploading(false)
    }
  }

  const createChat = async () => {
    try {
      const res = await axios.post('/api/chats', { title: 'New Chat' })
      setChats([res.data, ...chats])
      setCurrentChatId(res.data.id)
      setMessages([])
    } catch (e) { console.error(e) }
  }

  const loadChat = async (chatId: string) => {
    setCurrentChatId(chatId)
    try {
      const res = await axios.get(`/api/chats/${chatId}`)
      setMessages(res.data)
    } catch (e) { console.error(e) }
  }

  const sendMessage = async (overrideMessage?: string) => {
    const messageToSend = overrideMessage || input;
    if (!messageToSend.trim() || !currentChatId) return;

    const userMsg = { role: 'USER', content: messageToSend }
    setMessages(prev => [...prev, userMsg])
    if (!overrideMessage) setInput('')
    
    try {
      const res = await axios.post(`/api/chats/${currentChatId}/messages`, { message: userMsg.content })
      const assistantMsg = { 
          role: 'ASSISTANT', 
          content: res.data.answer_markdown,
          sources: res.data.citations 
      }
      setMessages(prev => [...prev, assistantMsg])
    } catch (err) {
      alert('Failed to send message')
      console.error(err)
    }
  }

  const handleExampleSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const selectedQuestion = e.target.value;
    if (!selectedQuestion) return;
    
    setInput(selectedQuestion);
    // Reset select
    e.target.value = "";
  }

  return (
    <div className="container">
      <div className="sidebar">
        <h3>Documents</h3>
        <input type="file" onChange={handleUpload} disabled={uploading} />
        {uploading && <div>Uploading...</div>}
        <ul>
          {documents.map(d => (
            <li key={d.id}>{d.title} ({d.status})</li>
          ))}
        </ul>
        <hr />
        <h3>Chats</h3>
        <button onClick={createChat}>+ New Chat</button>
        <ul>
          {chats.map(c => (
            <li key={c.id} onClick={() => loadChat(c.id)} 
                style={{fontWeight: c.id === currentChatId ? 'bold' : 'normal', cursor: 'pointer'}}>
              {c.title}
            </li>
          ))}
        </ul>
      </div>
      <div className="main">
        {currentChatId ? (
          <>
            <div className="chat-messages">
              {messages.map((m, i) => (
                <div key={i} className={`message ${m.role.toLowerCase()}`}>
                  <Markdown>{m.content}</Markdown>
                  {m.sources && m.sources.length > 0 && (
                    <div className="sources">
                      <strong>Sources:</strong>
                      {m.sources.map((s: any, idx: number) => (
                        <div key={idx} className="source-item">
                           Doc: {s.document_id ? s.document_id.substring(0,8) : 'Unknown'}... "{s.quote}"
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
            <div className="input-area">
              <select onChange={handleExampleSelect} defaultValue="" style={{marginBottom: '10px', width: '100%', padding: '5px'}}>
                <option value="" disabled>Examples</option>
                {exampleQuestions.map((q, i) => (
                  <option key={i} value={q}>{q}</option>
                ))}
              </select>
              <textarea value={input} onChange={e => setInput(e.target.value)} />
              <button onClick={() => sendMessage()}>Send</button>
            </div>
          </>
        ) : (
          <div>Select or create a chat</div>
        )}
      </div>
    </div>
  )
}

export default App
