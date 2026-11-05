const SE_PUEDE_TAPAR = typeof CSS !== 'undefined' && CSS.supports('-webkit-text-security', 'disc');

const bloquear = (e) => e.preventDefault();

export default function CampoClave({ campo, ...props }) {
  return (
    <input
      {...props}
      type={SE_PUEDE_TAPAR ? 'text' : 'password'}
      className={SE_PUEDE_TAPAR ? 'clave-tapada' : undefined}
      data-campo={campo}
      autoComplete={SE_PUEDE_TAPAR ? 'off' : 'new-password'}
      autoCorrect="off"
      autoCapitalize="off"
      spellCheck={false}
      onCopy={bloquear}
      onCut={bloquear}
      onDragStart={bloquear}
    />
  );
}
