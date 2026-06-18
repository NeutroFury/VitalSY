package com.jonesys.vitalsy.service;

import com.jonesys.vitalsy.model.EscalaDosisFija;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.EscalaDosisFijaRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class PautaExcelService {

    private final EscalaDosisFijaRepository escalaDosisFijaRepository;
    private final UsuarioRepository usuarioRepository;

    public PautaExcelService(EscalaDosisFijaRepository escalaDosisFijaRepository, UsuarioRepository usuarioRepository) {
        this.escalaDosisFijaRepository = escalaDosisFijaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public int procesarPautaExcel(Integer usuarioId, MultipartFile file) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));

        List<EscalaDosisFija> nuevaPauta = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {

            // Validar que existan las 4 pestañas requeridas
            validarPestanasObligatorias(workbook);

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String rawName = sheet.getSheetName().trim();
                
                // Mapear nombre de la pestaña al enum/string de la base de datos
                String momentoDia = mapearMomentoDia(rawName);
                if (momentoDia == null) {
                    continue; // Pestañas no requeridas se ignoran (si hubiera), pero ya validamos las 4 obligatorias
                }

                Iterator<Row> rowIterator = sheet.iterator();
                if (!rowIterator.hasNext()) {
                    throw new IllegalArgumentException("La pestaña '" + rawName + "' está vacía.");
                }

                // Fila 1: Cabeceras (Carbohidratos)
                Row headerRow = rowIterator.next();
                List<Double> carbohidratosColumnas = new ArrayList<>();

                // Empezamos desde la columna 1 (la 0 es para los rangos de glicemia)
                for (int col = 1; col < headerRow.getLastCellNum(); col++) {
                    Cell cell = headerRow.getCell(col);
                    String headerValue = getCellValueAsString(cell).toLowerCase().replaceAll("[^0-9.]", "").trim();
                    if (headerValue.isEmpty()) {
                        throw new IllegalArgumentException("Formato inválido en cabecera de Carbohidratos. Pestaña '" + rawName + "', Fila 1, Columna " + (col + 1));
                    }
                    try {
                        carbohidratosColumnas.add(Double.parseDouble(headerValue));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Formato inválido numérico en cabecera de Carbohidratos. Pestaña '" + rawName + "', Fila 1, Columna " + (col + 1));
                    }
                }

                // Filas de datos (matriz Glicemia x Carbohidratos)
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    Cell rangoCell = row.getCell(0);
                    
                    String rangoString = getCellValueAsString(rangoCell).trim();
                    if (rangoString.isEmpty()) {
                        // Podría ser una fila vacía al final, ignoramos si toda la fila está vacía
                        if (isRowEmpty(row)) continue;
                        throw new IllegalArgumentException("Rango de glicemia vacío. Pestaña '" + rawName + "', Fila " + (row.getRowNum() + 1) + ", Columna 1");
                    }

                    int glicemiaMin = 0;
                    int glicemiaMax = 999;

                    try {
                        if (rangoString.contains("-")) {
                            String[] partes = rangoString.split("-");
                            glicemiaMin = Integer.parseInt(partes[0].replaceAll("[^0-9]", "").trim());
                            glicemiaMax = Integer.parseInt(partes[1].replaceAll("[^0-9]", "").trim());
                        } else if (rangoString.contains("+") || rangoString.toLowerCase().contains("o más") || rangoString.toLowerCase().contains("o mas") || rangoString.contains(">")) {
                            glicemiaMin = Integer.parseInt(rangoString.replaceAll("[^0-9]", "").trim());
                        } else if (rangoString.contains("<")) {
                            glicemiaMax = Integer.parseInt(rangoString.replaceAll("[^0-9]", "").trim());
                        } else {
                            throw new IllegalArgumentException("Rango desconocido.");
                        }
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Formato de rango de glicemia inválido ('" + rangoString + "'). Pestaña '" + rawName + "', Fila " + (row.getRowNum() + 1) + ", Columna 1");
                    }

                    for (int col = 1; col < row.getLastCellNum(); col++) {
                        int index = col - 1;
                        if (index < carbohidratosColumnas.size()) {
                            Cell dosisCell = row.getCell(col);
                            String dosisString = getCellValueAsString(dosisCell).trim();
                            
                            if (dosisString.isEmpty()) {
                                throw new IllegalArgumentException("Celda de dosis vacía. Pestaña '" + rawName + "', Fila " + (row.getRowNum() + 1) + ", Columna " + (col + 1));
                            }
                            
                            try {
                                double dosis = Double.parseDouble(dosisString.replaceAll("[^0-9.]", ""));
                                EscalaDosisFija registro = new EscalaDosisFija();
                                registro.setUsuario(usuario);
                                registro.setNombreComidaPersonalizado(momentoDia);
                                registro.setGlicemiaMin(glicemiaMin);
                                registro.setGlicemiaMax(glicemiaMax);
                                registro.setCarbohidratosGr(carbohidratosColumnas.get(index));
                                registro.setDosisInsulina(dosis);
                                
                                nuevaPauta.add(registro);
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("Formato de dosis inválido ('" + dosisString + "'). Pestaña '" + rawName + "', Fila " + (row.getRowNum() + 1) + ", Columna " + (col + 1));
                            }
                        }
                    }
                }
            }

            if (nuevaPauta.isEmpty()) {
                throw new IllegalArgumentException("El archivo no contiene datos de pauta médica.");
            }

            // Todo válido (FAIL-FAST superado) -> Borramos registros anteriores e insertamos nuevos
            escalaDosisFijaRepository.deleteAllByUsuario_Id(usuarioId);
            escalaDosisFijaRepository.saveAll(nuevaPauta);

            return nuevaPauta.size();

        } catch (IllegalArgumentException e) {
            throw e; // Lanzar las excepciones de validación nativas directamente
        } catch (Exception e) {
            throw new RuntimeException("Error fatal procesando el archivo Excel: " + e.getMessage(), e);
        }
    }

    private void validarPestanasObligatorias(Workbook workbook) {
        List<String> hojasRequeridas = List.of("desayuno", "almuerzo", "once-cena (sin ejercicio)", "once-cena (con ejercicio)");
        List<String> hojasActuales = new ArrayList<>();
        
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            hojasActuales.add(workbook.getSheetName(i).trim().toLowerCase());
        }

        for (String requerida : hojasRequeridas) {
            boolean found = hojasActuales.stream().anyMatch(h -> h.contains(requerida.replace("once-cena", "once")));
            if (!found && !hojasActuales.contains(requerida)) {
                // Validación flexible
                boolean flexibleMatch = false;
                for (String actual : hojasActuales) {
                    if (actual.contains(requerida.split(" ")[0])) {
                        flexibleMatch = true; break;
                    }
                }
                if (!flexibleMatch) {
                    throw new IllegalArgumentException("El archivo Excel no contiene la pestaña obligatoria: " + requerida);
                }
            }
        }
    }

    private String mapearMomentoDia(String rawName) {
        String lower = rawName.toLowerCase();
        if (lower.contains("sin ejercicio")) return "ONCE_CENA_SIN_EJERCICIO";
        if (lower.contains("con ejercicio")) return "ONCE_CENA_CON_EJERCICIO";
        if (lower.contains("desayuno")) return "DESAYUNO";
        if (lower.contains("almuerzo")) return "ALMUERZO";
        return null;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // Si el número es 15.0, POI puede devolver "15.0" o "15".
                return String.valueOf(cell.getNumericCellValue());
            default:
                return "";
        }
    }
    
    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != org.apache.poi.ss.usermodel.CellType.BLANK) {
                String val = getCellValueAsString(cell).trim();
                if (!val.isEmpty()) return false;
            }
        }
        return true;
    }
}
