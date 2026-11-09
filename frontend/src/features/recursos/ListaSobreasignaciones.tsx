import type { AlertaSobreasignacion } from "../../types/recursos";
import { fecha } from "../proyectos/formato";
import { horas } from "./formato";

interface Props {
  alertas: AlertaSobreasignacion[];
}

/** Lista de días en que un profesional supera su capacidad (B-10). */
export default function ListaSobreasignaciones({ alertas }: Props) {
  return (
    <div>
      {alertas.length === 0 ? (
        <p className="vacio">Ningún profesional supera su capacidad diaria.</p>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th>Profesional</th>
              <th>Fecha</th>
              <th className="num">Carga</th>
              <th className="num">Capacidad</th>
              <th className="num">Exceso</th>
              <th>Tareas</th>
            </tr>
          </thead>
          <tbody>
            {alertas.map((a) => (
              <tr key={a.id}>
                <td>{a.profesional}</td>
                <td>{fecha(a.fecha)}</td>
                <td className="num">{horas(a.cargaHoras)}</td>
                <td className="num">{horas(a.capacidadHorasDia)}</td>
                <td className="num">{horas(a.excesoHoras)}</td>
                <td style={{ display: "flex", flexWrap: "wrap", gap: 4 }}>
                  {a.tareas.map((t) => (
                    <span key={t.id} className="etiqueta" title={t.nombre}>
                      {t.codigo}
                    </span>
                  ))}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
