/** Validate bindings before token verification or any writes. Never return secret values. */
export function configurationError(env) {
    const required = [
        'FIREBASE_PROJECT_ID', 'FIREBASE_SERVICE_ACCOUNT_EMAIL',
        'FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY', 'ALLOWED_ORIGINS',
    ];
    const missing = required.filter((name) => !env[name]?.trim());
    return missing.length ? `Missing Worker configuration: ${missing.join(', ')}` : null;
}
