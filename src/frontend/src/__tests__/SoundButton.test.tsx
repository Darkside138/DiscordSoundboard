import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { SoundButton } from '../components/SoundButton'

const sound = {
  id: 's1',
  name: 'air_horn',
  category: 'memes',
  url: '/sounds/air_horn.mp3',
} as any

describe('SoundButton', () => {
  it('formats name and calls onPlay when enabled', async () => {
    const onPlay = vi.fn()
    const onToggleFavorite = vi.fn()
    const onContextMenu = vi.fn()
    render(
      <SoundButton
        sound={sound}
        isFavorite={false}
        isTopPlayed={false}
        isRecentlyAdded={false}
        onPlay={onPlay}
        onToggleFavorite={onToggleFavorite}
        onContextMenu={onContextMenu}
        theme="dark"
      />
    )
    const btn = screen.getByRole('button', { name: /air horn/i })
    expect(btn).toBeInTheDocument()
    await userEvent.click(btn)
    expect(onPlay).toHaveBeenCalledTimes(1)
  })

  it('is disabled when disabled prop is true and does not call onPlay', async () => {
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
        disabled
      />
    )
    const btn = screen.getByRole('button') as HTMLButtonElement
    expect(btn.disabled).toBe(true)
    await userEvent.click(btn)
    expect(onPlay).not.toHaveBeenCalled()
  })
})
