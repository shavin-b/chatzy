import packageInfo from '../../package.json';
export const brand = Object.freeze({
  product: 'Chatzy',
  version: packageInfo.version,
  console: 'Chatzy Admin Console',
  company: 'SHAVIN SOLUTIONS (PVT) LTD',
  tagline: 'Manage Your Chatzy Business.',
  supportEmail: import.meta.env.VITE_SUPPORT_EMAIL || '',
  supportPhone: import.meta.env.VITE_SUPPORT_PHONE || '',
  privacyPolicyUrl: import.meta.env.VITE_PRIVACY_POLICY_URL || '',
  websiteUrl: import.meta.env.VITE_WEBSITE_URL || '',
});
