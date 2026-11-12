/** Server policy: Support can view admin data but cannot issue remote commands. */
export function canSendDeviceCommand(role: unknown): boolean {
  return role === 'Super Admin' || role === 'Admin';
}
