import React from 'react'
import clsx from 'clsx'

type BadgeProps = React.HTMLAttributes<HTMLSpanElement> & {
	color?: 'brand' | 'accent' | 'neutral' | 'success' | 'warning' | 'danger'
}

export const Badge: React.FC<BadgeProps> = ({ color = 'neutral', className, ...props }) => {
	const styles: Record<NonNullable<BadgeProps['color']>, string> = {
		brand: 'bg-brand-100 text-brand-800',
		accent: 'bg-accent-100 text-accent-800',
		neutral: 'bg-slate-100 text-slate-800 dark:bg-slate-700/60 dark:text-slate-100',
		success: 'bg-emerald-100 text-emerald-800',
		warning: 'bg-yellow-100 text-yellow-800',
		danger: 'bg-red-100 text-red-800',
	}
	return <span className={clsx('inline-flex items-center rounded-full px-2 py-0.5 text-xs', styles[color], className)} {...props} />
}


