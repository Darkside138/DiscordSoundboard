import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { SoundButton } from '../SoundButton'

const sound = {
  id: '1',
  name: 'hello_world-Sound',
  category: 'misc',
  url: '/sounds/hello.mp3',
  displayName: null as string | null,
}

describe('SoundButton', () => {
  it('formats and displays sound name', () => {
    const onPlay = vi.fn()
    render(
      <SoundButton
        sound={sound}
        isFavorite={false}
        isTopPlayed={false}
        isRecentlyAdded={false}
        onPlay={onPlay}
        onToggleFavorite={() => {}}
        onContextMenu={() => {}}
        theme="dark"
      />
    )
    expect(screen.getByText('Hello World Sound')).toBeInTheDocument()
  })

  it('calls onPlay when clicked and not disabled', () => {
    const onPlay = vi.fn()
    render(
      <SoundButton
        sound={sound}
        isFavorite={false}
        isTopPlayed={false}
        isRecentlyAdded={false}
        onPlay={onPlay}
        onToggleFavorite={() => {}}
        onContextMenu={() => {}}
        theme="light"
      />
    )
    const button = screen.getByRole('button', { name: /hello world sound/i })
    fireEvent.click(button)
    expect(onPlay).toHaveBeenCalledTimes(1)
  })

  it('is disabled with proper title when disabled is true', () => {
    const onPlay = vi.fn()
    render(
      <SoundButton
        sound={sound}
        isFavorite={false}
        isTopPlayed={false}
        isRecentlyAdded={false}
        onPlay={onPlay}
        onToggleFavorite={() => {}}
        onContextMenu={() => {}}
        theme="dark"
        disabled
      />
    )
    const button = screen.getByRole('button')
    expect(button).toBeDisabled()
    expect(button).toHaveAttribute('title', expect.stringMatching(/must be in voice channel/i))
    fireEvent.click(button)
    expect(onPlay).not.toHaveBeenCalled()
  })

  it('shows stop local overlay that triggers onStopLocalPlayback without triggering onPlay', () => {
    const onPlay = vi.fn()
    const onStopLocalPlayback = vi.fn()
    render(
      <SoundButton
        sound={sound}
        isFavorite={false}
        isTopPlayed={false}
        isRecentlyAdded={false}
        onPlay={onPlay}
        onToggleFavorite={() => {}}
        onContextMenu={() => {}}
        theme="dark"
        isLocallyPlaying
        onStopLocalPlayback={onStopLocalPlayback}
      />
    )
    const overlayBtn = screen.getByRole('button', { name: /click to stop local playback/i })
    fireEvent.click(overlayBtn)
    expect(onStopLocalPlayback).toHaveBeenCalledTimes(1)
    expect(onPlay).not.toHaveBeenCalled()
  })
})
