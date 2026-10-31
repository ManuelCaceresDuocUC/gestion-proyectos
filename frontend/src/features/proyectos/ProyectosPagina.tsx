import { useSearchParams } from "react-router-dom";
import ListadoProyectos from "./ListadoProyectos";
import ProyectoDetalle from "./ProyectoDetalle";

/** /proyectos muestra el listado; /proyectos?id=3 muestra el detalle del proyecto 3. */
export default function ProyectosPagina() {
  const [params, setParams] = useSearchParams();
  const id = Number(params.get("id"));
  if (Number.isInteger(id) && id > 0) {
    return <ProyectoDetalle id={id} volver={() => setParams({})} />;
  }
  return <ListadoProyectos abrir={(nuevo) => setParams({ id: String(nuevo) })} />;
}