import React, { useMemo, useState, useEffect } from 'react'
import { useAppState } from '../state/AppState'
import { useAuth } from '../state/AuthState'
import { SubscriptionInfo } from '../components/SubscriptionInfo'
import { subscriptionService } from '../services/subscriptionService'
import { countPendingReportRequestsForOwner } from '../services/api'

const DashboardPage: React.FC = () => {
	const { cases, notifications } = useAppState()
	const { user } = useAuth()
	const [renewalMessage, setRenewalMessage] = useState<string | null>(null)
	const [pendingRequestsCount, setPendingRequestsCount] = useState<number>(0)
	
	// Charger les vraies données du backend
	useEffect(() => {
		const loadPendingRequestsCount = async () => {
			if (user?.name) {
				try {
					const count = await countPendingReportRequestsForOwner(user.name)
					setPendingRequestsCount(count)
				} catch (error) {
					console.error('Erreur lors du chargement des demandes en attente:', error)
					setPendingRequestsCount(0)
				}
			}
		}
		
		loadPendingRequestsCount()
	}, [user?.name])
	
	const counts = useMemo(() => {
		const mine = cases.length
		const total = cases.length // TODO: backend multi‑tenancy
		const pending = pendingRequestsCount // Utiliser les vraies données du backend
		const pendingTotal = pending // TODO: global metrics backend
		return { mine, total, pending, pendingTotal }
	}, [cases, pendingRequestsCount])

	const handleRequestRenewal = async () => {
		if (!user?.id) return
		
		// Demander confirmation
		if (!confirm('Êtes-vous sûr de vouloir demander le renouvellement de votre abonnement ?')) {
			return
		}
		
		try {
			const result = await subscriptionService.requestRenewal(user.id)
			if (result.success) {
				setRenewalMessage('Demande de renouvellement envoyée avec succès ! L\'administrateur sera notifié.')
				// Recharger les données utilisateur après la demande
				setTimeout(() => {
					window.location.reload()
				}, 2000)
			} else {
				setRenewalMessage('Erreur lors de la demande de renouvellement')
			}
		} catch (error) {
			setRenewalMessage('Erreur lors de la demande de renouvellement')
		}
	}
	return (
		<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-16">
			<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
				<div className="rounded-xl border border-slate-200 dark:border-slate-700 p-5">
					<div className="text-xs text-slate-500">Dossiers déposés (maison)</div>
					<div className="mt-1 text-2xl font-semibold">{counts.mine}</div>
				</div>
				<div className="rounded-xl border border-slate-200 dark:border-slate-700 p-5">
					<div className="text-xs text-slate-500">Dossiers déposés (total)</div>
					<div className="mt-1 text-2xl font-semibold">{counts.total}</div>
				</div>
				<div className="rounded-xl border border-slate-200 dark:border-slate-700 p-5">
					<div className="text-xs text-slate-500">Demandes d’accès en attente (maison)</div>
					<div className="mt-1 text-2xl font-semibold">{counts.pending}</div>
				</div>
				<div className="rounded-xl border border-slate-200 dark:border-slate-700 p-5">
					<div className="text-xs text-slate-500">Demandes d’accès en attente (total)</div>
					<div className="mt-1 text-2xl font-semibold">{counts.pendingTotal}</div>
				</div>
			</div>
			
			{/* Informations d'abonnement */}
			{user && (
				<div className="mt-6">
					{user.role === 'admin' ? (
						<div className="bg-white rounded-lg border border-gray-200 p-6">
							<h3 className="text-lg font-semibold text-gray-900 mb-4">
								👑 Abonnement Administrateur
							</h3>
							<div className="space-y-4">
								<div className="flex items-center justify-between">
									<span className="text-sm font-medium text-gray-700">Statut :</span>
									<span className="px-3 py-1 rounded-full text-xs font-medium border text-blue-600 bg-blue-50 border-blue-200">
										Abonnement Permanent
									</span>
								</div>
								<div className="flex items-center justify-between">
									<span className="text-sm font-medium text-gray-700">Type :</span>
									<span className="text-sm text-gray-900">Administrateur</span>
								</div>
								<div className="flex items-center justify-between">
									<span className="text-sm font-medium text-gray-700">Expiration :</span>
									<span className="text-sm text-green-600 font-semibold">Jamais</span>
								</div>
								<div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-md">
									<div className="flex items-center">
										<svg className="h-5 w-5 text-blue-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
											<path fillRule="evenodd" d="M6.267 3.455a3.066 3.066 0 001.745-.723 3.066 3.066 0 013.976 0 3.066 3.066 0 001.745.723 3.066 3.066 0 012.812 2.812c.051.643.304 1.254.723 1.745a3.066 3.066 0 010 3.976 3.066 3.066 0 00-.723 1.745 3.066 3.066 0 01-2.812 2.812 3.066 3.066 0 00-1.745.723 3.066 3.066 0 01-3.976 0 3.066 3.066 0 00-1.745-.723 3.066 3.066 0 01-2.812-2.812 3.066 3.066 0 00-.723-1.745 3.066 3.066 0 010-3.976 3.066 3.066 0 00.723-1.745 3.066 3.066 0 012.812-2.812zm7.44 5.252a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
										</svg>
										<span className="text-sm text-blue-800">
											En tant qu'administrateur, vous avez un abonnement permanent qui ne nécessite pas de renouvellement.
										</span>
									</div>
								</div>
							</div>
						</div>
					) : user.subscriptionStartDate && user.subscriptionEndDate ? (
						<>
							<SubscriptionInfo
								subscriptionStartDate={user.subscriptionStartDate}
								subscriptionEndDate={user.subscriptionEndDate}
								subscriptionActive={user.subscriptionActive || false}
								subscriptionStatus={user.subscriptionStatus || 'ACTIVE'}
								daysUntilExpiration={user.daysUntilExpiration || 0}
								showRenewalButton={user.role !== 'admin'}
								onRequestRenewal={handleRequestRenewal}
							/>
							
							{/* Message de renouvellement */}
							{renewalMessage && (
								<div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-md">
									<div className="flex items-center">
										<svg className="h-5 w-5 text-blue-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
											<path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
										</svg>
										<span className="text-sm text-blue-800">{renewalMessage}</span>
									</div>
								</div>
							)}
						</>
					) : (
						<div className="bg-white rounded-lg border border-gray-200 p-6">
							<h3 className="text-lg font-semibold text-gray-900 mb-4">
								📅 Informations d'Abonnement
							</h3>
							<div className="text-sm text-gray-600">
								Les informations d'abonnement ne sont pas encore disponibles.
							</div>
						</div>
					)}
				</div>
			)}
			
			<div className="mt-6 rounded-xl border border-slate-200 dark:border-slate-700 p-5">
				<div className="text-sm font-semibold">Dernières notifications</div>
				<ul className="mt-3 space-y-2 text-sm">
					{notifications.slice(0, 5).map(n => (
						<li key={n.id} className="flex items-center justify-between">
							<span>{n.title}</span>
							<span className="text-xs text-slate-500">{new Date(n.createdAt).toLocaleTimeString()}</span>
						</li>
					))}
					{notifications.length === 0 && (
						<li className="text-slate-500">Aucune notification</li>
					)}
				</ul>
			</div>
		</div>
	)
}

export default DashboardPage


