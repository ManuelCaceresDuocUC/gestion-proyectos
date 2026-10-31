import type { ReactNode } from "react";

interface Props {
  titulo: string;
  children: ReactNode;
  textoConfirmar: string;
  peligro?: boolean;
  alConfirmar: () => void;
  alCancelar: () => void;
}

/** Diálogo de confirmación (cerrar proyecto, eliminar tarea...). */
export default function Confirmar({ titulo, children, textoConfirmar, peligro, alConfirmar, alCancelar }: Props) {
  return (
    <div className="modal-fondo" role="dialog" aria-modal="true" aria-label={titulo}>
      <div className="modal">
        <h3>{titulo}</h3>
        <div>{children}</div>
        <div className="acciones">
          <button className={`boton ${peligro ? "peligro" : "primario"}`} onClick={alConfirmar}>{textoConfirmar}</button>
          <button className="boton" onClick={alCancelar}>Cancelar</button>
        </div>
      </div>
    </div>
  );
}
