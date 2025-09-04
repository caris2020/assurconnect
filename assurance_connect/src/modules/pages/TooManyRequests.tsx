import React from 'react'
import { NavLink } from 'react-router-dom'

const TooManyRequestsPage: React.FC = () => {
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow border border-gray-200 dark:border-gray-700 p-8 text-center">
        <div className="text-6xl mb-4">⏳</div>
        <h1 className="text-2xl font-bold mb-2">Trop de requêtes</h1>
        <p className="text-gray-600 dark:text-gray-300 mb-6">
          Vous avez effectué trop d'actions en peu de temps. Veuillez patienter une minute puis réessayer.
        </p>
        <div className="flex items-center justify-center gap-3">
          <button onClick={() => window.location.reload()} className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md text-sm">
            Réessayer
          </button>
          <NavLink to="/" className="text-sm text-blue-600 hover:underline">
            Retour au tableau de bord
          </NavLink>
        </div>
      </div>
    </div>
  )
}

export default TooManyRequestsPage


