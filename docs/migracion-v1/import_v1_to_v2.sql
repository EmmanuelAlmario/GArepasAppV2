-- =============================================================
-- Importacion V1 -> V2 (solo catalogo: insumos, recetas, productos, detalle_recetas)
-- Prerrequisito (ver docs/DESPLIEGUE-RAILWAY.md, Paso 3):
--   1) Esquema v1_staging cargado con las 4 tablas V1 (catalogo).
--   2) V2 arrancada una vez (crea el esquema public vacio con ddl-auto=update).
-- Ejecutar en una transaccion. Idempotencia: solo debe correrse sobre tablas vacias.
-- =============================================================
BEGIN;

-- Guardia: no importar si ya hay datos en la V2
DO $$
BEGIN
  IF (SELECT count(*) FROM public.insumos) + (SELECT count(*) FROM public.recetas)
     + (SELECT count(*) FROM public.productos) > 0 THEN
    RAISE EXCEPTION 'Abortado: public ya contiene datos de catalogo';
  END IF;
END $$;

-- 1) Insumos (se excluye id 21 "Random": dato de prueba sin uso en recetas)
INSERT INTO public.insumos
  (id, nombre, categoria, marca, stock_actual, stock_minimo, precio_por_gramo, unidad_medida, activo, version)
SELECT id, btrim(nombre), btrim(categoria), btrim(marca), stock_actual, stock_minimo,
       precio_por_gramo, unidad_medida, activo, COALESCE(version, 0)
FROM v1_staging.insumos
WHERE id <> 21;

-- 2) Recetas
INSERT INTO public.recetas (id, nombre, descripcion)
SELECT id, btrim(nombre), NULLIF(btrim(descripcion), '')
FROM v1_staging.recetas;

-- 3) Productos (stock = snapshot actual de V1; ventas/producciones no se migran)
INSERT INTO public.productos
  (id, nombre, stock_actual, stock_minimo, precio_venta, receta_id, activo, version)
SELECT id, btrim(nombre), stock_actual, stock_minimo, precio_venta, receta_id, activo, COALESCE(version, 0)
FROM v1_staging.productos;

-- 4) Detalle de recetas (solo las que apuntan a insumos importados)
INSERT INTO public.detalle_recetas (id, receta_id, insumo_id, cantidad, unidad_medida)
SELECT d.id, d.receta_id, d.insumo_id, d.cantidad, d.unidad_medida
FROM v1_staging.detalle_recetas d
WHERE d.insumo_id IN (SELECT id FROM public.insumos);

-- 5) Ajustar secuencias para que los proximos inserts no choquen con ids importados
SELECT setval(pg_get_serial_sequence('public.insumos', 'id'),       COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM public.insumos;
SELECT setval(pg_get_serial_sequence('public.recetas', 'id'),       COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM public.recetas;
SELECT setval(pg_get_serial_sequence('public.productos', 'id'),     COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM public.productos;
SELECT setval(pg_get_serial_sequence('public.detalle_recetas', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM public.detalle_recetas;

COMMIT;

-- 6) Limpieza: el esquema temporal ya no se necesita (ejecutar tras verificar el catalogo)
-- DROP SCHEMA v1_staging CASCADE;
