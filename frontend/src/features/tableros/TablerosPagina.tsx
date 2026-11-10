// Layout compartido. Proyecto: B-21. Cambio y filtros: C-12 y C-13.
import TableroProyecto from "./TableroProyecto";

export default function TablerosPagina() {
  return (
    <section>
      <h2>Tableros de comando</h2>
      <TableroProyecto />
      <section>
        <h3>Tablero de cambio</h3>
        <p className="vacio">Pendiente de implementar (C-12, C-13).</p>
      </section>
    </section>
  );
}
