import type { TableroCambio as Datos } from "../../types/tableros";
import { dinero } from "../proyectos/formato";
import {
  ESTADOS_SOLICITUD,
  ETIQUETA_ALERTA,
  ETIQUETA_ESTADO,
  cuentaEstado,
  decimales,
  textoCumplimiento,
  textoSemaforo,
  textoValorKpi,
} from "./formato";

interface Props {
  datos: Datos;
  moneda: string;
}

function Cifra({ etiqueta, valor }: { etiqueta: string; valor: string | number }) {
  return (
    <div className="tarjeta">
      <span className="vacio">{etiqueta}</span>
      <strong>{valor}</strong>
    </div>
  );
}

/** Lectura del tablero de cambio (C-12). Los números salen del servidor. */
export default function TableroCambio({ datos, moneda }: Props) {
  const { conteos, etapas, costos, tiempos, efectividad, alertas, kpi } = datos;
  const total = conteos.total;

  return (
    <section>
      <h3>Gestión del cambio</h3>
      <div className="cifras">
        <Cifra etiqueta="Registrados" valor={conteos.total} />
        <Cifra etiqueta="En curso" valor={conteos.enCurso} />
        <Cifra etiqueta="Cerrados" valor={cuentaEstado(conteos.porEstado, "CERRADO")} />
        <Cifra etiqueta="Rechazados" valor={cuentaEstado(conteos.porEstado, "RECHAZADO")} />
        <Cifra etiqueta="Atrasados" valor={conteos.atrasados} />
        <Cifra etiqueta="Adopción (etapa 10)" valor={conteos.adoptados} />
      </div>

      <h4>Por estado</h4>
      {total > 0 && (
        <div className="barra-estados" aria-hidden="true">
          {ESTADOS_SOLICITUD.map((estado) => {
            const n = cuentaEstado(conteos.porEstado, estado);
            if (n === 0) return null;
            return (
              <span
                key={estado}
                className={`estado-${estado}`}
                style={{ flex: n }}
                title={`${ETIQUETA_ESTADO[estado]}: ${n}`}
              />
            );
          })}
        </div>
      )}
      <table className="tabla">
        <thead>
          <tr>
            <th>Estado</th>
            <th className="num">Cantidad</th>
          </tr>
        </thead>
        <tbody>
          {ESTADOS_SOLICITUD.map((estado) => (
            <tr key={estado}>
              <td><span className={`etiqueta estado-${estado}`}>{ETIQUETA_ESTADO[estado]}</span></td>
              <td className="num">{cuentaEstado(conteos.porEstado, estado)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <dl className="datos">
        <div>
          <dt>Avance promedio de etapas</dt>
          <dd>{etapas.avancePromedio} %</dd>
        </div>
        <div>
          <dt>Riesgo alto / crítico</dt>
          <dd>{conteos.riesgoAlto} / {conteos.riesgoCritico}</dd>
        </div>
        <div>
          <dt>Con bloqueo / correctiva</dt>
          <dd>{conteos.conBloqueo} / {conteos.conCorrectiva}</dd>
        </div>
        <div>
          <dt>Presupuesto vs costo real (cerradas)</dt>
          <dd>{dinero(costos.presupuestoCerradas, moneda)} / {dinero(costos.costoRealCerradas, moneda)}</dd>
        </div>
        <div>
          <dt>Días de atraso (total / promedio)</dt>
          <dd>{tiempos.diasAtrasoTotal} / {decimales(tiempos.diasAtrasoPromedio)}</dd>
        </div>
        <div>
          <dt>Efectividad (efectivo / parcial / no / sin dato)</dt>
          <dd>{efectividad.efectivo} / {efectividad.parcial} / {efectividad.noEfectivo} / {efectividad.sinDato}</dd>
        </div>
      </dl>

      <h4>Alertas</h4>
      {alertas.length === 0 ? (
        <p className="vacio">Sin alertas de atraso, bloqueo, riesgo crítico ni KPI en rojo.</p>
      ) : (
        <ul className="lista-alertas">
          {alertas.map((alerta, i) => (
            <li key={`${alerta.tipo}-${alerta.solicitudCodigo ?? alerta.mensaje}-${i}`}>
              <span className={`etiqueta alerta-${alerta.tipo}`}>{ETIQUETA_ALERTA[alerta.tipo] ?? alerta.tipo}</span>
              {" "}{alerta.mensaje}
            </li>
          ))}
        </ul>
      )}

      <h4>KPI de cambio</h4>
      {kpi.length === 0 ? (
        <p className="aviso">Sin snapshot. Los KC aparecen después de un recálculo (cambiar una duración, un recurso o aplicar un cambio).</p>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th>Código</th>
              <th>Nombre</th>
              <th className="num">Valor</th>
              <th>Semáforo</th>
              <th className="num">Cumplimiento</th>
            </tr>
          </thead>
          <tbody>
            {kpi.map((fila) => (
              <tr key={fila.id}>
                <td>{fila.codigo}</td>
                <td>{fila.nombre}{fila.alerta ? " · alerta" : ""}</td>
                <td className="num">{textoValorKpi(fila.valor, fila.unidad, fila.noCalculable)}</td>
                <td>
                  <span className={`etiqueta semaforo-${fila.noCalculable ? "NULO" : fila.semaforo ?? "NULO"}`}>
                    {textoSemaforo(fila.semaforo, fila.noCalculable)}
                  </span>
                </td>
                <td className="num">{textoCumplimiento(fila.cumplimiento)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
