import React, { createContext, useContext, useMemo, useState, useEffect } from 'react'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export type UserRole = 'admin' | 'point_focal'

export type User = {
	id?: number
	name: string
	role: UserRole
	email?: string
	insuranceCompany?: string
	firstName?: string
	lastName?: string
	// Informations d'abonnement
	subscriptionStartDate?: string
	subscriptionEndDate?: string
	subscriptionActive?: boolean
	subscriptionStatus?: string
	daysUntilExpiration?: number
	lastRenewalRequestDate?: string
}

type AuthState = {
	user: User | null
	isAuthenticated: boolean
	login: (username: string, insuranceCompany: string, password: string) => Promise<boolean>
	logout: () => void
}

const AuthContext = createContext<AuthState | undefined>(undefined)

export const AuthStateProvider: React.FC<React.PropsWithChildren> = ({ children }) => {
	const [user, setUser] = useState<User | null>(null)
	const [isAuthenticated, setIsAuthenticated] = useState(false)

	// Vérifier si l'utilisateur est déjà connecté au chargement
	useEffect(() => {
		const savedUser = localStorage.getItem('assurance_user')
		if (savedUser) {
			try {
				const userData = JSON.parse(savedUser)
				setUser(userData)
				setIsAuthenticated(true)
			} catch (error) {
				console.error('Erreur lors du chargement de l\'utilisateur:', error)
				localStorage.removeItem('assurance_user')
			}
		}
	}, [])

	const login = async (username: string, insuranceCompany: string, password: string): Promise<boolean> => {
		try {
			// Appel à l'API de connexion (JWT)
			const response = await fetch(`${API_BASE}/api/auth/login`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
				},
				body: JSON.stringify({
					username,
					insuranceCompany: (insuranceCompany || '').trim(),
					password
				})
			})

			if (response.ok) {
				const payload = await response.json()
				const token = payload?.token as string | undefined
				const u = payload?.user ?? payload
				if (token) {
					localStorage.setItem('assurance_token', token)
				}
				const user: User = {
					id: u?.id,
					name: u?.username ?? u?.name,
					role: (u?.role === 'ADMIN' ? 'admin' : 'point_focal') as UserRole,
					email: u?.email,
					insuranceCompany: u?.insuranceCompany,
					firstName: u?.firstName,
					lastName: u?.lastName,
					// Informations d'abonnement
					subscriptionStartDate: u?.subscriptionStartDate,
					subscriptionEndDate: u?.subscriptionEndDate,
					subscriptionActive: u?.subscriptionActive,
					subscriptionStatus: u?.subscriptionStatus,
					daysUntilExpiration: u?.daysUntilExpiration,
					lastRenewalRequestDate: u?.lastRenewalRequestDate
				}
				
				setUser(user)
				setIsAuthenticated(true)
				localStorage.setItem('assurance_user', JSON.stringify(user))
				return true
			} else {
				console.error('Erreur de connexion:', response.statusText)
				return false
			}
		} catch (error) {
			console.error('Erreur lors de la connexion:', error)
			return false
		}
	}

	const logout = () => {
		setUser(null)
		setIsAuthenticated(false)
		localStorage.removeItem('assurance_user')
		localStorage.removeItem('assurance_token')
	}

	const value = useMemo(() => ({ 
		user, 
		isAuthenticated, 
		login, 
		logout
	}), [user, isAuthenticated])

	return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthState => {
	const ctx = useContext(AuthContext)
	if (!ctx) throw new Error('useAuth must be used within AuthStateProvider')
	return ctx
}


