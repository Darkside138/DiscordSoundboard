import React from 'react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, fireEvent, within } from '@testing-library/react'
import { UsersOverlay } from '../UsersOverlay'

// Mock sonner toast
const toastErrorMock = vi.fn()
vi.mock('sonner', () => ({
  toast: {
    error: (...args: any[]) => toastErrorMock(...args),
  },
}))

// Mock API endpoints and utilities
vi.mock('../../config', () => ({
  API_ENDPOINTS: {
    DISCORD_USERS: 'http://localhost/api/discord/users',
  },
}))

const fetchWithAuthMock = vi.fn()
vi.mock('../../utils/api', () => ({
  fetchWithAuth: (...args: any[]) => fetchWithAuthMock(...args),
  getAuthHeaders: () => ({ Authorization: 'Bearer test' }),
}))

const paged = (content: any[], { number = 0, totalPages = 1, size = 10, totalElements = content.length } = {}) => ({
  content,
  page: { number, totalPages, size, totalElements },
})

const mkUser = (over: Partial<any> = {}) => ({
  id: 'u1',
  username: 'Alice',
  entranceSound: null,
  leaveSound: null,
  ...over,
})

describe('UsersOverlay', () => {
  beforeEach(() => {
    fetchWithAuthMock.mockReset()
    toastErrorMock.mockReset()
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    ;(console.error as any).mockRestore?.()
  })

  const baseProps = () => ({
    isOpen: true,
    onClose: vi.fn(),
    theme: 'dark' as const,
    sounds: [
      { id: 's1', name: 'Hello' },
      { id: 's2', name: 'World' },
    ],
  })

  it('fetches users on open with default sort and paginates', async () => {
    // First call page 0, totalPages 3
    fetchWithAuthMock.mockResolvedValueOnce(new Response(JSON.stringify(paged([mkUser()], { number: 0, totalPages: 3 })), { status: 200 }))
    // Second call page 1
    fetchWithAuthMock.mockResolvedValueOnce(new Response(JSON.stringify(paged([mkUser({ id: 'u2', username: 'Bob' })], { number: 1, totalPages: 3 })), { status: 200 }))

    render(<UsersOverlay {...baseProps()} />)

    await waitFor(() => expect(fetchWithAuthMock).toHaveBeenCalled())
    // Verify default sort params present
    const firstUrl = new URL((fetchWithAuthMock.mock.calls[0] as any)[0] as string)
    expect(firstUrl.searchParams.get('page')).toBe('0')
    expect(firstUrl.searchParams.get('size')).toBe('10')
    expect(firstUrl.searchParams.get('sortBy')).toBe('username')
    expect(firstUrl.searchParams.get('sortDir')).toBe('asc')

    // Click next page
    const nextBtn = screen.getByLabelText(/next page/i)
    expect(nextBtn).not.toBeDisabled()
    fireEvent.click(nextBtn)

    await waitFor(() => expect(fetchWithAuthMock).toHaveBeenCalledTimes(2))
    const secondUrl = new URL((fetchWithAuthMock.mock.calls[1] as any)[0] as string)
    expect(secondUrl.searchParams.get('page')).toBe('1')
  })

  it('cycles sorting asc -> desc and resets to page 0', async () => {
    // Respond for each fetch call
    fetchWithAuthMock.mockResolvedValue(new Response(JSON.stringify(paged([mkUser()], { number: 0, totalPages: 1 })), { status: 200 }))

    render(<UsersOverlay {...baseProps()} />)
    await waitFor(() => expect(fetchWithAuthMock).toHaveBeenCalledTimes(1))

    // Click username header: currently asc -> should go desc
    const usernameHeader = screen.getByText(/username/i)
    fireEvent.click(usernameHeader)
    await waitFor(() => expect(fetchWithAuthMock).toHaveBeenCalledTimes(2))
    const url2 = new URL((fetchWithAuthMock.mock.calls[1] as any)[0] as string)
    expect(url2.searchParams.get('sortBy')).toBe('username')
    expect(url2.searchParams.get('sortDir')).toBe('desc')
    expect(url2.searchParams.get('page')).toBe('0') // reset to first page
  })

  it('starts editing entrance sound, saves successfully, and closes editor', async () => {
    fetchWithAuthMock
      // initial list
      .mockResolvedValueOnce(new Response(JSON.stringify(paged([mkUser({ id: 'u1', username: 'Alice', entranceSound: null })], { totalPages: 1 })), { status: 200 }))
      // PATCH save
      .mockResolvedValueOnce(new Response('', { status: 200 }))

    render(<UsersOverlay {...baseProps()} />)
    await waitFor(() => expect(fetchWithAuthMock).toHaveBeenCalledTimes(1))

    // Click entrance sound cell button to enter edit mode
    const row = screen.getByText('Alice').closest('tr') as HTMLElement
    const entranceCell = row.children[1] as HTMLElement
    const entranceBtn = within(entranceCell).getByRole('button')
    fireEvent.click(entranceBtn)

    // Select a sound and save
    const select = within(entranceCell).getByRole('combobox') as HTMLSelectElement
    fireEvent.change(select, { target: { value: 's2' } })
    const saveButton = within(entranceCell).getByRole('button', { name: /save/i })
    fireEvent.click(saveButton)

    await waitFor(() => expect(fetchWithAuthMock).toHaveBeenCalledTimes(2))
    const patchUrl = (fetchWithAuthMock.mock.calls[1] as any)[0] as string
    expect(patchUrl).toMatch(/\/api\/discord\/users\/u1\?/)
    expect(patchUrl).toMatch(/entranceSound=s2/)

    // Editor closed (button visible again instead of select)
    expect(within(entranceCell).queryByRole('combobox')).toBeNull()
  })

  it('shows toast error on failed save and stays in edit mode', async () => {
    fetchWithAuthMock
      .mockResolvedValueOnce(new Response(JSON.stringify(paged([mkUser({ id: 'u1', username: 'Alice', entranceSound: null })], { totalPages: 1 })), { status: 200 }))
      .mockResolvedValueOnce(new Response('boom', { status: 500 }))

    render(<UsersOverlay {...baseProps()} />)
    await waitFor(() => expect(fetchWithAuthMock).toHaveBeenCalledTimes(1))

    const row = screen.getByText('Alice').closest('tr') as HTMLElement
    const entranceCell = row.children[1] as HTMLElement
    fireEvent.click(within(entranceCell).getByRole('button'))

    const select = within(entranceCell).getByRole('combobox') as HTMLSelectElement
    fireEvent.change(select, { target: { value: 's1' } })
    fireEvent.click(within(entranceCell).getByRole('button', { name: /save/i }))

    await waitFor(() => expect(fetchWithAuthMock).toHaveBeenCalledTimes(2))
    expect(toastErrorMock).toHaveBeenCalled()
    // Still in edit mode (select present)
    expect(within(entranceCell).getByRole('combobox')).toBeInTheDocument()
  })
})
