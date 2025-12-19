import '@testing-library/jest-dom'

// Provide a minimal EventSource mock for JSDOM environment (used by DiscordUsersList)
if (!(globalThis as any).EventSource) {
  class MockEventSource {
    url: string
    withCredentials = false
    onopen: ((ev: MessageEvent) => any) | null = null
    onmessage: ((ev: MessageEvent) => any) | null = null
    onerror: ((ev: MessageEvent) => any) | null = null
    constructor(url: string) {
      this.url = url
    }
    addEventListener() {}
    removeEventListener() {}
    close() {}
  }
  ;(globalThis as any).EventSource = MockEventSource as any
}
