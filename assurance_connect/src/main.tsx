import React from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { App } from './modules/App'

// Global 429 redirect wrapper
const __originalFetch = window.fetch.bind(window)
window.fetch = async (...args) => {
  const res = await __originalFetch(...args as Parameters<typeof fetch>)
  if (res && res.status === 429 && window.location.pathname !== '/429') {
    try { await res.text() } catch {}
    window.location.assign('/429')
  }
  return res
}

const root = createRoot(document.getElementById('root')!)
root.render(
	<React.StrictMode>
		<App />
	</React.StrictMode>
)


