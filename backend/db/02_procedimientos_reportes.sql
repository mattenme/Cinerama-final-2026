-- =====================================================================
-- Cinerama - Procedimientos de reportes
--
-- Ejecutar con:
--   sqlplus cineramaBD/123@localhost:1521/XE @db/02_procedimientos_reportes.sql
--
-- Procedimientos usados por ReporteService con SimpleJdbcCall:
--   SP_RESUMEN_FUNCION, SP_VENTAS_POR_PELICULA, SP_OCUPACION_POR_FUNCION,
--   SP_TOP_CLIENTES, SP_COMPARACION_PERIODOS, SP_CLIENTE_VS_PROMEDIO.
-- Los listados devuelven SYS_REFCURSOR.
-- =====================================================================

SET SERVEROUTPUT ON

CREATE OR REPLACE PROCEDURE SP_RESUMEN_FUNCION (
    p_id_funcion        IN  NUMBER,
    p_butacas           OUT NUMBER,
    p_ingresos_entradas OUT NUMBER,
    p_ingresos_dulceria OUT NUMBER,
    p_total             OUT NUMBER
) IS
BEGIN
    -- Se agrega por compra y luego se suma, para no repetir TOTAL_PRODUCTOS
    -- una vez por butaca.
    SELECT NVL(SUM(x.butacas), 0),
           NVL(SUM(x.entradas), 0),
           NVL(SUM(x.dulceria), 0)
      INTO p_butacas, p_ingresos_entradas, p_ingresos_dulceria
      FROM (
            SELECT c.ID_COMPRA,
                   COUNT(d.ID_DETALLE)            AS butacas,
                   NVL(SUM(d.PRECIO), 0)          AS entradas,
                   NVL(MAX(c.TOTAL_PRODUCTOS), 0) AS dulceria
              FROM COMPRA c
              LEFT JOIN COMPRA_DETALLE d ON d.ID_COMPRA = c.ID_COMPRA
             WHERE c.ID_FUNCION = p_id_funcion
               AND UPPER(c.ESTADO) IN ('COMPLETADA', 'CONFIRMADA')
             GROUP BY c.ID_COMPRA
           ) x;

    p_total := NVL(p_ingresos_entradas, 0) + NVL(p_ingresos_dulceria, 0);
EXCEPTION
    WHEN OTHERS THEN
        p_butacas := 0;
        p_ingresos_entradas := 0;
        p_ingresos_dulceria := 0;
        p_total := 0;
        RAISE;
END SP_RESUMEN_FUNCION;
/

CREATE OR REPLACE PROCEDURE SP_VENTAS_POR_PELICULA (
    p_cursor OUT SYS_REFCURSOR
) IS
BEGIN
    OPEN p_cursor FOR
        SELECT p.ID_PELICULA,
               p.TITULO,
               COUNT(g.ID_COMPRA)              AS COMPRAS,
               NVL(SUM(g.BUTACAS), 0)          AS BUTACAS,
               NVL(SUM(g.ENTRADAS), 0)         AS ENTRADAS,
               NVL(SUM(g.DULCERIA), 0)         AS DULCERIA,
               NVL(SUM(g.ENTRADAS), 0) + NVL(SUM(g.DULCERIA), 0) AS TOTAL
          FROM PELICULA p
          JOIN FUNCION f ON f.ID_PELICULA = p.ID_PELICULA
          JOIN (
                SELECT c.ID_FUNCION,
                       c.ID_COMPRA,
                       COUNT(d.ID_DETALLE)            AS BUTACAS,
                       NVL(SUM(d.PRECIO), 0)          AS ENTRADAS,
                       NVL(MAX(c.TOTAL_PRODUCTOS), 0) AS DULCERIA
                  FROM COMPRA c
                  JOIN COMPRA_DETALLE d ON d.ID_COMPRA = c.ID_COMPRA
                 WHERE UPPER(c.ESTADO) IN ('COMPLETADA', 'CONFIRMADA')
                 GROUP BY c.ID_FUNCION, c.ID_COMPRA
               ) g ON g.ID_FUNCION = f.ID_FUNCION
         GROUP BY p.ID_PELICULA, p.TITULO
         ORDER BY NVL(SUM(g.ENTRADAS), 0) + NVL(SUM(g.DULCERIA), 0) DESC;
END SP_VENTAS_POR_PELICULA;
/

CREATE OR REPLACE PROCEDURE SP_OCUPACION_POR_FUNCION (
    p_cursor OUT SYS_REFCURSOR
) IS
BEGIN
    OPEN p_cursor FOR
        SELECT f.ID_FUNCION,
               p.TITULO,
               s.NOMBRE AS SALA,
               f.HORA_INICIO,
               (SELECT COUNT(*) FROM ASIENTO a WHERE a.ID_SALA = f.ID_SALA) AS CAPACIDAD,
               COUNT(DISTINCT c.ID_COMPRA) AS COMPRAS,
               COUNT(d.ID_DETALLE)         AS VENDIDAS,
               CASE
                   WHEN (SELECT COUNT(*) FROM ASIENTO a WHERE a.ID_SALA = f.ID_SALA) = 0
                       THEN 0
                   ELSE ROUND(COUNT(d.ID_DETALLE) * 100 /
                              (SELECT COUNT(*) FROM ASIENTO a WHERE a.ID_SALA = f.ID_SALA), 1)
               END AS OCUPACION_PCT
          FROM FUNCION f
          JOIN PELICULA p ON p.ID_PELICULA = f.ID_PELICULA
          JOIN SALA s ON s.ID_SALA = f.ID_SALA
          LEFT JOIN COMPRA c
                 ON c.ID_FUNCION = f.ID_FUNCION
                AND UPPER(c.ESTADO) IN ('COMPLETADA', 'CONFIRMADA')
          LEFT JOIN COMPRA_DETALLE d ON d.ID_COMPRA = c.ID_COMPRA
         GROUP BY f.ID_FUNCION, p.TITULO, s.NOMBRE, f.HORA_INICIO, f.ID_SALA
         ORDER BY f.HORA_INICIO DESC;
END SP_OCUPACION_POR_FUNCION;
/

CREATE OR REPLACE PROCEDURE SP_TOP_CLIENTES (
    p_limite IN  NUMBER,
    p_cursor OUT SYS_REFCURSOR
) IS
    v_limite NUMBER := NVL(p_limite, 5);
BEGIN
    IF v_limite < 1 THEN
        v_limite := 5;
    END IF;

    OPEN p_cursor FOR
        WITH por_compra AS (
            SELECT c.ID_CLIENTE,
                   c.ID_COMPRA,
                   c.FECHA_COMPRA,
                   COUNT(d.ID_DETALLE)            AS butacas,
                   NVL(SUM(d.PRECIO), 0)          AS entradas,
                   NVL(MAX(c.TOTAL_PRODUCTOS), 0) AS dulceria
              FROM COMPRA c
              LEFT JOIN COMPRA_DETALLE d ON d.ID_COMPRA = c.ID_COMPRA
             WHERE UPPER(c.ESTADO) IN ('COMPLETADA', 'CONFIRMADA')
             GROUP BY c.ID_CLIENTE, c.ID_COMPRA, c.FECHA_COMPRA
        ),
        por_cliente AS (
            SELECT ID_CLIENTE,
                   COUNT(*)                 AS compras,
                   SUM(butacas)             AS butacas,
                   SUM(entradas)            AS entradas,
                   SUM(dulceria)            AS dulceria,
                   SUM(entradas + dulceria) AS total,
                   MAX(FECHA_COMPRA)        AS ultima_compra
              FROM por_compra
             GROUP BY ID_CLIENTE
        )
        SELECT posicion, dni, nombre, compras, butacas, entradas, dulceria,
               total, ticket_promedio, diferencia_promedio, vs_promedio_pct,
               ultima_compra
          FROM (
                SELECT ROW_NUMBER() OVER (ORDER BY pc.total DESC, pc.compras DESC) AS posicion,
                       cl.DNI                           AS dni,
                       cl.NOMBRE                        AS nombre,
                       pc.compras,
                       pc.butacas,
                       pc.entradas,
                       pc.dulceria,
                       pc.total,
                       ROUND(pc.total / pc.compras, 2)  AS ticket_promedio,
                       ROUND(pc.total - pr.promedio, 2) AS diferencia_promedio,
                       CASE
                           WHEN pr.promedio = 0 THEN 0
                           ELSE ROUND((pc.total - pr.promedio) * 100 / pr.promedio, 1)
                       END                              AS vs_promedio_pct,
                       pc.ultima_compra
                  FROM por_cliente pc
                  JOIN CLIENTE cl ON cl.ID_CLIENTE = pc.ID_CLIENTE
                 CROSS JOIN (
                       SELECT NVL(AVG(total), 0) AS promedio
                         FROM por_cliente
                 ) pr
               )
         WHERE posicion <= v_limite
         ORDER BY posicion;
END SP_TOP_CLIENTES;
/

CREATE OR REPLACE PROCEDURE SP_COMPARACION_PERIODOS (
    p_dias   IN  NUMBER,
    p_cursor OUT SYS_REFCURSOR
) IS
    v_dias NUMBER := NVL(p_dias, 30);
BEGIN
    IF v_dias < 1 THEN
        v_dias := 30;
    END IF;

    OPEN p_cursor FOR
        WITH por_compra AS (
            SELECT c.ID_COMPRA,
                   c.FECHA_COMPRA,
                   COUNT(d.ID_DETALLE)            AS butacas,
                   NVL(SUM(d.PRECIO), 0)          AS entradas,
                   NVL(MAX(c.TOTAL_PRODUCTOS), 0) AS dulceria
              FROM COMPRA c
              LEFT JOIN COMPRA_DETALLE d ON d.ID_COMPRA = c.ID_COMPRA
             WHERE UPPER(c.ESTADO) IN ('COMPLETADA', 'CONFIRMADA')
               AND c.FECHA_COMPRA >= SYSTIMESTAMP - (2 * v_dias)
               AND c.FECHA_COMPRA <= SYSTIMESTAMP
             GROUP BY c.ID_COMPRA, c.FECHA_COMPRA
        ),
        totales AS (
            SELECT
                NVL(SUM(CASE WHEN FECHA_COMPRA >= SYSTIMESTAMP - v_dias THEN 1 ELSE 0 END), 0) AS compras_actual,
                NVL(SUM(CASE WHEN FECHA_COMPRA <  SYSTIMESTAMP - v_dias THEN 1 ELSE 0 END), 0) AS compras_anterior,
                NVL(SUM(CASE WHEN FECHA_COMPRA >= SYSTIMESTAMP - v_dias THEN butacas ELSE 0 END), 0) AS butacas_actual,
                NVL(SUM(CASE WHEN FECHA_COMPRA <  SYSTIMESTAMP - v_dias THEN butacas ELSE 0 END), 0) AS butacas_anterior,
                NVL(SUM(CASE WHEN FECHA_COMPRA >= SYSTIMESTAMP - v_dias THEN entradas ELSE 0 END), 0) AS entradas_actual,
                NVL(SUM(CASE WHEN FECHA_COMPRA <  SYSTIMESTAMP - v_dias THEN entradas ELSE 0 END), 0) AS entradas_anterior,
                NVL(SUM(CASE WHEN FECHA_COMPRA >= SYSTIMESTAMP - v_dias THEN dulceria ELSE 0 END), 0) AS dulceria_actual,
                NVL(SUM(CASE WHEN FECHA_COMPRA <  SYSTIMESTAMP - v_dias THEN dulceria ELSE 0 END), 0) AS dulceria_anterior,
                NVL(SUM(CASE WHEN FECHA_COMPRA >= SYSTIMESTAMP - v_dias THEN entradas + dulceria ELSE 0 END), 0) AS total_actual,
                NVL(SUM(CASE WHEN FECHA_COMPRA <  SYSTIMESTAMP - v_dias THEN entradas + dulceria ELSE 0 END), 0) AS total_anterior
              FROM por_compra
        )
        SELECT metrica,
               actual,
               anterior,
               actual - anterior AS variacion,
               CASE
                   WHEN anterior = 0 THEN NULL
                   ELSE ROUND((actual - anterior) * 100 / anterior, 1)
               END AS variacion_pct
          FROM (
                SELECT 'COMPRAS'  AS metrica, compras_actual  AS actual, compras_anterior  AS anterior, 1 AS orden FROM totales
                UNION ALL
                SELECT 'BUTACAS'  AS metrica, butacas_actual  AS actual, butacas_anterior  AS anterior, 2 AS orden FROM totales
                UNION ALL
                SELECT 'ENTRADAS' AS metrica, entradas_actual AS actual, entradas_anterior AS anterior, 3 AS orden FROM totales
                UNION ALL
                SELECT 'DULCERIA' AS metrica, dulceria_actual AS actual, dulceria_anterior AS anterior, 4 AS orden FROM totales
                UNION ALL
                SELECT 'TOTAL'    AS metrica, total_actual    AS actual, total_anterior    AS anterior, 5 AS orden FROM totales
               )
         ORDER BY orden;
END SP_COMPARACION_PERIODOS;
/

CREATE OR REPLACE PROCEDURE SP_CLIENTE_VS_PROMEDIO (
    p_id_cliente    IN  NUMBER,
    p_dni           OUT VARCHAR2,
    p_nombre        OUT VARCHAR2,
    p_compras       OUT NUMBER,
    p_total_cliente OUT NUMBER,
    p_promedio      OUT NUMBER,
    p_diferencia    OUT NUMBER,
    p_porcentaje    OUT NUMBER,
    p_posicion      OUT NUMBER,
    p_clientes      OUT NUMBER
) IS
    CURSOR c_gastos IS
        WITH por_compra AS (
            SELECT c.ID_CLIENTE,
                   c.ID_COMPRA,
                   NVL(SUM(d.PRECIO), 0)          AS entradas,
                   NVL(MAX(c.TOTAL_PRODUCTOS), 0) AS dulceria
              FROM COMPRA c
              LEFT JOIN COMPRA_DETALLE d ON d.ID_COMPRA = c.ID_COMPRA
             WHERE UPPER(c.ESTADO) IN ('COMPLETADA', 'CONFIRMADA')
             GROUP BY c.ID_CLIENTE, c.ID_COMPRA
        )
        SELECT ID_CLIENTE,
               COUNT(*)                 AS compras,
               SUM(entradas + dulceria) AS total
          FROM por_compra
         GROUP BY ID_CLIENTE
         ORDER BY total DESC;

    v_reg        c_gastos%ROWTYPE;
    v_suma       NUMBER := 0;
    v_clientes   NUMBER := 0;
    v_posicion   NUMBER := 0;
    v_compras    NUMBER := 0;
    v_total      NUMBER := 0;
    v_encontrado BOOLEAN := FALSE;
BEGIN
    BEGIN
        SELECT DNI, NOMBRE
          INTO p_dni, p_nombre
          FROM CLIENTE
         WHERE ID_CLIENTE = p_id_cliente;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20001, 'CLIENTE_NO_EXISTE');
    END;

    OPEN c_gastos;
    LOOP
        FETCH c_gastos INTO v_reg;
        EXIT WHEN c_gastos%NOTFOUND;

        v_clientes := v_clientes + 1;
        v_suma := v_suma + v_reg.total;

        IF v_reg.ID_CLIENTE = p_id_cliente THEN
            v_posicion := v_clientes;
            v_compras := v_reg.compras;
            v_total := v_reg.total;
            v_encontrado := TRUE;
        END IF;
    END LOOP;
    CLOSE c_gastos;

    p_clientes := v_clientes;
    p_compras := NVL(v_compras, 0);
    p_total_cliente := NVL(v_total, 0);
    p_promedio := CASE
                      WHEN v_clientes = 0 THEN 0
                      ELSE ROUND(v_suma / v_clientes, 2)
                  END;
    p_diferencia := ROUND(p_total_cliente - p_promedio, 2);
    p_porcentaje := CASE
                        WHEN p_promedio = 0 THEN 0
                        ELSE ROUND((p_total_cliente - p_promedio) * 100 / p_promedio, 1)
                    END;
    p_posicion := CASE WHEN v_encontrado THEN v_posicion ELSE NULL END;

EXCEPTION
    WHEN OTHERS THEN
        IF c_gastos%ISOPEN THEN
            CLOSE c_gastos;
        END IF;
        RAISE;
END SP_CLIENTE_VS_PROMEDIO;
/

-- El paquete anterior ya no se usa.
BEGIN
    EXECUTE IMMEDIATE 'DROP PACKAGE PKG_CINERAMA_REPORTES';
    DBMS_OUTPUT.PUT_LINE('Paquete PKG_CINERAMA_REPORTES eliminado.');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -4043 THEN
            RAISE;
        END IF;
END;
/

COMMIT;

SELECT object_name, object_type, status
  FROM user_objects
 WHERE object_name IN ('SP_RESUMEN_FUNCION',
                       'SP_VENTAS_POR_PELICULA',
                       'SP_OCUPACION_POR_FUNCION',
                       'SP_TOP_CLIENTES',
                       'SP_COMPARACION_PERIODOS',
                       'SP_CLIENTE_VS_PROMEDIO')
 ORDER BY object_name;
