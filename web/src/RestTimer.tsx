import { useEffect, useState } from 'react';
import { Timer, Play, Pause, RotateCcw } from 'lucide-react';
export default function RestTimer() {
    const [duration, setDuration] = useState(90), [remaining, setRemaining] = useState(90), [end, setEnd] = useState<number | null>(null);
    useEffect(() => { if (end === null)
        return; const tick = () => { const left = Math.max(0, Math.ceil((end - Date.now()) / 1000)); setRemaining(left); if (left === 0)
        setEnd(null); }; tick(); const id = setInterval(tick, 250); return () => clearInterval(id); }, [end]);
    return <section className="panel timer"><div><h3><Timer size={19}/> Descanso entre series</h3><p className="hint">El contador mantiene el tiempo aunque cambies de pestaña del navegador.</p></div><strong role="timer" aria-label="Tiempo restante">{String(Math.floor(remaining / 60)).padStart(2, '0')}:{String(remaining % 60).padStart(2, '0')}</strong><select aria-label="Duración del descanso" value={duration} onChange={e => { const n = Number(e.target.value); setDuration(n); setRemaining(n); setEnd(null); }}>{[30, 60, 90, 120, 180].map(n => <option key={n} value={n}>{n} segundos</option>)}</select><button type="button" aria-label={end ? 'Pausar descanso' : 'Iniciar descanso'} onClick={() => end ? setEnd(null) : setEnd(Date.now() + (remaining || duration) * 1000)}>{end ? <Pause size={18}/> : <Play size={18}/>}</button><button type="button" aria-label="Reiniciar descanso" onClick={() => { setEnd(null); setRemaining(duration); }}><RotateCcw size={18}/></button>{remaining === 0 && <span role="status">Descanso terminado</span>}</section>;
}
