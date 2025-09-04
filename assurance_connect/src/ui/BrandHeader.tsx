import React from 'react'
import { useAuth } from '../modules/state/AuthState'

export const BrandHeader: React.FC<React.PropsWithChildren> = ({ children }) => {
	const { user } = useAuth()
	return (
		<header className="bg-gradient-to-r from-brand-700 to-brand-500 text-white">
			<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
				<div className="flex items-center gap-3">
					<div className="h-9 w-9 rounded-lg bg-white/15 grid place-items-center">🔐</div>
					<div>
						<div className="text-lg font-semibold">Assurance Connect</div>
						<div className="text-xs text-white/80">Plateforme anti‑fraude</div>
					</div>
				</div>
				<div className="flex items-center gap-4">
					{children}
					<div className="hidden md:flex items-center gap-2 text-xs">
						<span className="opacity-80">{user?.firstName || user?.name}</span>
						<span className="opacity-60">({user?.role === 'admin' ? 'Administrateur' : 'Point Focal'})</span>
					</div>
				</div>
			</div>
		</header>
	)
}


