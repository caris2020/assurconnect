import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '../../ui'

const ForgotPasswordPage: React.FC = () => {
    const navigate = useNavigate()
    const [email, setEmail] = useState('')
    const [insuranceCompany, setInsuranceCompany] = useState('')
    const [loading, setLoading] = useState(false)
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setLoading(true)
        setMessage(null)

        try {
            const response = await fetch('http://localhost:8080/api/password-reset/request', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    email,
                    insuranceCompany
                })
            })

            const data = await response.json()

            if (data.success) {
                setMessage({ type: 'success', text: data.message })
                setEmail('')
                setInsuranceCompany('')
            } else {
                setMessage({ type: 'error', text: data.message })
            }
        } catch (error) {
            setMessage({ type: 'error', text: 'Erreur lors de la demande de réinitialisation' })
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
            <div className="sm:mx-auto sm:w-full sm:max-w-md">
                <div className="flex justify-center">
                    <div className="w-12 h-12 bg-blue-600 rounded-lg flex items-center justify-center">
                        <span className="text-white text-2xl">🔒</span>
                    </div>
                </div>
                <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
                    Mot de passe oublié
                </h2>
                <p className="mt-2 text-center text-sm text-gray-600">
                    Entrez votre email et votre compagnie d'assurance pour recevoir un lien de réinitialisation
                </p>
            </div>

            <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
                <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
                    <form className="space-y-6" onSubmit={handleSubmit}>
                        <div>
                            <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                                Adresse email
                            </label>
                            <div className="mt-1">
                                <input
                                    id="email"
                                    name="email"
                                    type="email"
                                    autoComplete="email"
                                    required
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                                    placeholder="votre@email.com"
                                />
                            </div>
                        </div>

                        <div>
                            <label htmlFor="insuranceCompany" className="block text-sm font-medium text-gray-700">
                                Compagnie d'assurance
                            </label>
                            <div className="mt-1">
                                <select
                                    id="insuranceCompany"
                                    name="insuranceCompany"
                                    required
                                    value={insuranceCompany}
                                    onChange={(e) => setInsuranceCompany(e.target.value)}
                                    className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                                >
                                    <option value="">Sélectionnez votre compagnie</option>
                                    <option value="ALLIANZ">ALLIANZ</option>
                                    <option value="AXA">AXA</option>
                                    <option value="GROUPAMA">GROUPAMA</option>
                                    <option value="MAIF">MAIF</option>
                                    <option value="MACIF">MACIF</option>
                                    <option value="MMA">MMA</option>
                                    <option value="GMF">GMF</option>
                                    <option value="CRAS">CRAS</option>
                                    <option value="SAAR ASSURANCE">SAAR ASSURANCE</option>
                                    <option value="SUNU">SUNU</option>
                                    <option value="Système">Système</option>
                                </select>
                            </div>
                        </div>

                        {message && (
                            <div className={`rounded-md p-4 ${
                                message.type === 'success' 
                                    ? 'bg-green-50 border border-green-200' 
                                    : 'bg-red-50 border border-red-200'
                            }`}>
                                <p className={`text-sm ${
                                    message.type === 'success' ? 'text-green-800' : 'text-red-800'
                                }`}>
                                    {message.text}
                                </p>
                            </div>
                        )}

                        <div>
                            <Button
                                type="submit"
                                disabled={loading}
                                className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {loading ? 'Envoi en cours...' : 'Envoyer le lien de réinitialisation'}
                            </Button>
                        </div>

                        <div className="text-center">
                            <button
                                type="button"
                                onClick={() => navigate('/login')}
                                className="text-sm text-blue-600 hover:text-blue-500"
                            >
                                Retour à la connexion
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    )
}

export default ForgotPasswordPage
