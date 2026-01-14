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
  const [selectedDocId, setSelectedDocId] = useState<string | null>(null)
  const [docContent, setDocContent] = useState<string>('')
  const [sidebarWidth, setSidebarWidth] = useState(250)
  const [previewWidth, setPreviewWidth] = useState(400)
  const [isResizing, setIsResizing] = useState<'sidebar' | 'preview' | null>(null)

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
    
    try {
      for (let i = 0; i < e.target.files.length; i++) {
        const file = e.target.files[i]
        const formData = new FormData()
        formData.append('file', file)
        await axios.post('/api/docs', formData)
      }
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
    // Reset select and close dropdown
    e.target.value = "";
    e.target.blur();
  }

  const handleDocClick = async (docId: string) => {
    setSelectedDocId(docId)
    try {
      const res = await axios.get(`/api/docs/${docId}/raw`)
      setDocContent(res.data)
    } catch (e) { console.error(e) }
  }

  const handleMouseDown = (type: 'sidebar' | 'preview') => {
    setIsResizing(type)
  }

  const handleMouseMove = (e: MouseEvent) => {
    if (isResizing === 'sidebar') {
      setSidebarWidth(Math.max(200, Math.min(800, e.clientX)))
    } else if (isResizing === 'preview') {
      setPreviewWidth(Math.max(300, Math.min(1000, window.innerWidth - e.clientX)))
    }
  }

  const handleMouseUp = () => {
    setIsResizing(null)
  }

  useEffect(() => {
    if (isResizing) {
      document.addEventListener('mousemove', handleMouseMove)
      document.addEventListener('mouseup', handleMouseUp)
      return () => {
        document.removeEventListener('mousemove', handleMouseMove)
        document.removeEventListener('mouseup', handleMouseUp)
      }
    }
  }, [isResizing])

  return (
    <div className="container">
      <div className="sidebar" style={{width: sidebarWidth}}>
        <h3>Documents</h3>
        <input type="file" onChange={handleUpload} disabled={uploading} multiple webkitdirectory />
        {uploading && <div>Uploading...</div>}
        <ul>
          {documents.map(d => (
            <li key={d.id} onClick={() => handleDocClick(d.id)} 
                style={{cursor: 'pointer', fontWeight: d.id === selectedDocId ? 'bold' : 'normal'}}>
              {d.title} ({d.status})
            </li>
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
      <div className="resizer" onMouseDown={() => handleMouseDown('sidebar')}></div>
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
                        <div key={idx} className="source-item" onClick={() => handleDocClick(s.document_id)}>
                           Doc: {s.document_id ? s.document_id.substring(0,8) : 'Unknown'}... "{s.quote}"
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
            <div className="input-area">
              <div className="examples-dropdown">
                <select onChange={handleExampleSelect} defaultValue="">
                  <option value="" disabled>Examples</option>
                  {exampleQuestions.map((q, i) => (
                    <option key={i} value={q}>{q}</option>
                  ))}
                </select>
              </div>
              <textarea value={input} onChange={e => setInput(e.target.value)} />
              <button onClick={() => sendMessage()}>Send</button>
            </div>
          </>
        ) : (
          <div>Select or create a chat</div>
        )}
      </div>
      {selectedDocId && (
        <>
          <div className="resizer" onMouseDown={() => handleMouseDown('preview')}></div>
          <div className="doc-preview" style={{width: previewWidth}}>
            <div className="doc-preview-header">
              <h3>Document Preview</h3>
              <button onClick={() => setSelectedDocId(null)}>×</button>
            </div>
            <div className="doc-preview-content">
              <Markdown>{docContent}</Markdown>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

export default App
