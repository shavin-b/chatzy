import { useState } from 'react';
import { getFunctions, httpsCallable } from 'firebase/functions';
import app from '../services/firebase';
import { useAuth } from '../hooks/useAuth';

type RequestRow = {requestId: string; deviceId: string; authUid: string; expiresAt: number};
export function DeviceEnrollmentApproval() {
  const {admin} = useAuth();
  const [requests,setRequests] = useState<RequestRow[]>([]);
  const [pairing,setPairing] = useState('');
  const [message,setMessage] = useState('');
  const [busy,setBusy] = useState(false);
  if (admin?.role !== 'Super Admin' || import.meta.env.VITE_DEVICE_ENROLLMENT_ENABLED !== 'true') return null;
  const functions = getFunctions(app,'us-central1');
  async function load() {
    setBusy(true);
    try {
      const result=await httpsCallable<unknown,{requests:RequestRow[]}>(functions,'listDeviceEnrollmentRequests')({});
      setRequests(result.data.requests);setMessage('Compare the full pairing ID with the physical device.');
    } catch {setMessage('Enrollment service unavailable or access denied. No approval was made.');}
    finally {setBusy(false);}
  }
  async function approve(row:RequestRow) {
    if (pairing.trim() !== row.requestId) return;
    setBusy(true);
    try {
      await httpsCallable(functions,'approveDeviceEnrollment')({requestId:row.requestId});
      setRequests(current=>current.filter(item=>item.requestId!==row.requestId));
      setPairing('');setMessage('Approved. Ask the device owner to check approval on the device.');
    } catch {setMessage('Approval rejected. Check expiry or conflicting installation ownership.');}
    finally {setBusy(false);}
  }
  return <section className="rounded-2xl border border-slate-800 bg-slate-900 p-5 space-y-3">
    <h2 className="text-lg font-semibold text-white">Device enrollment</h2>
    <p className="text-sm text-slate-400">Approve only after comparing the full pairing ID on the physical device. A familiar device ID alone does not prove ownership.</p>
    <button type="button" disabled={busy} onClick={load} className="rounded-lg bg-teal-700 px-4 py-2 text-white">Load pending requests</button>
    <p className="text-sm text-slate-300" role="status">{message}</p>
    <label className="block text-sm text-slate-300">Pairing ID copied from the physical device
      <input value={pairing} onChange={event=>setPairing(event.target.value)} autoComplete="off" className="mt-2 block w-full rounded-lg bg-slate-800 p-3" />
    </label>
    {requests.map(row=><div key={row.requestId} className="rounded-lg border border-slate-700 p-3 text-sm text-slate-300">
      <p>{row.deviceId}</p><p className="break-all">Pairing: {row.requestId}</p>
      <p>Expires: {new Date(row.expiresAt).toLocaleString()}</p>
      <button type="button" disabled={busy || pairing.trim()!==row.requestId} onClick={()=>approve(row)} className="mt-2 rounded-lg bg-teal-700 p-2 text-white disabled:opacity-40">Approve verified device</button>
    </div>)}
  </section>;
}
