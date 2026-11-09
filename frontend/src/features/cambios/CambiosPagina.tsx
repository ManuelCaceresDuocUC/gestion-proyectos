import { useSearchParams } from "react-router-dom";
import DetalleCambio from "./DetalleCambio";
import ListadoCambios from "./ListadoCambios";

/** /cambios: listado; /cambios?id=8: detalle (C-10). */
export default function CambiosPagina() {
  const [params, setParams] = useSearchParams();
  const id = Number(params.get("id"));
  if (Number.isInteger(id) && id > 0) {
    return <DetalleCambio id={id} volver={() => setParams({})} />;
  }
  return <ListadoCambios abrir={(nuevo) => setParams({ id: String(nuevo) })} />;
}
