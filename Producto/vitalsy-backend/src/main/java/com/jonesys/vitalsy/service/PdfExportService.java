package com.jonesys.vitalsy.service;

import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.dto.response.AgpDataResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfExportService {

    private final AgpStatisticsService agpStatisticsService;

    public PdfExportService(AgpStatisticsService agpStatisticsService) {
        this.agpStatisticsService = agpStatisticsService;
    }

    public ByteArrayInputStream generateGlucosePdf(Usuario usuario, List<GlucoseReading> readings, ZoneId userZone) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(26, 54, 93));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(113, 128, 150));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(45, 55, 72));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(45, 55, 72));
            Font boldBodyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(45, 55, 72));

            // Title
            Paragraph title = new Paragraph("Historial de Glucosa - VitalSY", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);

            // Subtitle / Date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String generationDate = ZonedDateTime.now(userZone).format(formatter);
            Paragraph subtitle = new Paragraph("Reporte generado el: " + generationDate, subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // User Info Section
            Paragraph userInfoTitle = new Paragraph("Información del Paciente", sectionFont);
            userInfoTitle.setSpacingAfter(8);
            document.add(userInfoTitle);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1, 1});
            infoTable.setSpacingAfter(20);

            PdfPCell cell1 = new PdfPCell(new Phrase("Nombre: " + usuario.getNombre(), bodyFont));
            cell1.setBorder(Rectangle.NO_BORDER);
            infoTable.addCell(cell1);

            PdfPCell cell2 = new PdfPCell(new Phrase("Email: " + usuario.getEmail(), bodyFont));
            cell2.setBorder(Rectangle.NO_BORDER);
            infoTable.addCell(cell2);

            document.add(infoTable);

            // --- AGP Section ---
            Paragraph agpTitle = new Paragraph("Perfil de Glucosa Ambulatorio (AGP)", sectionFont);
            agpTitle.setSpacingAfter(8);
            document.add(agpTitle);

            AgpDataResponse agpData = agpStatisticsService.getAgpData(usuario);
            if (agpData.getDiasAnalizados() != null && agpData.getDiasAnalizados() >= 3) {
                try {
                    Image agpImage = createAgpChartImage(agpData);
                    agpImage.setAlignment(Element.ALIGN_CENTER);
                    agpImage.scaleToFit(500, 250);
                    agpImage.setSpacingAfter(20);
                    document.add(agpImage);
                    
                    Paragraph agpSubtitle = new Paragraph("Análisis basado en los últimos " + agpData.getDiasAnalizados() + " días.", subtitleFont);
                    agpSubtitle.setAlignment(Element.ALIGN_CENTER);
                    agpSubtitle.setSpacingAfter(25);
                    document.add(agpSubtitle);
                } catch (Exception e) {
                    System.err.println("Error generando imagen AGP: " + e.getMessage());
                }
            } else {
                PdfPTable emptyAgpTable = new PdfPTable(1);
                emptyAgpTable.setWidthPercentage(100);
                emptyAgpTable.setSpacingAfter(25);
                PdfPCell emptyCell = new PdfPCell(new Phrase("Perfil de Glucosa Ambulatorio (AGP) no disponible: Se requieren más días de registro para un análisis estadístico válido.", subtitleFont));
                emptyCell.setBackgroundColor(new Color(247, 250, 252));
                emptyCell.setBorderColor(new Color(226, 232, 240));
                emptyCell.setPadding(15);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyAgpTable.addCell(emptyCell);
                document.add(emptyAgpTable);
            }

            // Statistics Section
            ZonedDateTime thirtyDaysAgo = ZonedDateTime.now().minusDays(30);
            List<GlucoseReading> last30DaysReadings = readings.stream()
                    .filter(r -> r.getFechaHora().isAfter(thirtyDaysAgo))
                    .toList();

            double mean = 0.0;
            double stdDev = 0.0;
            int totalReadings30Days = last30DaysReadings.size();

            if (totalReadings30Days > 0) {
                double sum = 0.0;
                for (GlucoseReading r : last30DaysReadings) {
                    sum += r.getValorMgdl();
                }
                mean = sum / totalReadings30Days;

                if (totalReadings30Days > 1) {
                    double sumSquaredDiff = 0.0;
                    for (GlucoseReading r : last30DaysReadings) {
                        double diff = r.getValorMgdl() - mean;
                        sumSquaredDiff += diff * diff;
                    }
                    stdDev = Math.sqrt(sumSquaredDiff / (totalReadings30Days - 1));
                }
            }

            Paragraph statsTitle = new Paragraph("Resumen Clínico (Últimos 30 días)", sectionFont);
            statsTitle.setSpacingAfter(8);
            document.add(statsTitle);

            PdfPTable statsTable = new PdfPTable(3);
            statsTable.setWidthPercentage(100);
            statsTable.setWidths(new float[]{1, 1, 1});
            statsTable.setSpacingAfter(25);

            Color cardBg = new Color(247, 250, 252);
            Color cardBorder = new Color(226, 232, 240);

            // Total Readings Card
            PdfPCell totalCell = new PdfPCell();
            totalCell.setBackgroundColor(cardBg);
            totalCell.setBorderColor(cardBorder);
            totalCell.setPadding(10);
            totalCell.addElement(new Paragraph("Total Lecturas", subtitleFont));
            totalCell.addElement(new Paragraph(String.valueOf(totalReadings30Days), titleFont));
            statsTable.addCell(totalCell);

            // Mean Card
            PdfPCell meanCell = new PdfPCell();
            meanCell.setBackgroundColor(cardBg);
            meanCell.setBorderColor(cardBorder);
            meanCell.setPadding(10);
            meanCell.addElement(new Paragraph("Promedio (Media)", subtitleFont));
            meanCell.addElement(new Paragraph(totalReadings30Days > 0 ? String.format("%.1f mg/dL", mean) : "N/A", titleFont));
            statsTable.addCell(meanCell);

            // StdDev Card
            PdfPCell stdDevCell = new PdfPCell();
            stdDevCell.setBackgroundColor(cardBg);
            stdDevCell.setBorderColor(cardBorder);
            stdDevCell.setPadding(10);
            stdDevCell.addElement(new Paragraph("Variabilidad (Desv. Est.)", subtitleFont));
            stdDevCell.addElement(new Paragraph(totalReadings30Days > 1 ? String.format("%.1f mg/dL", stdDev) : "N/A", titleFont));
            statsTable.addCell(stdDevCell);

            document.add(statsTable);

            // Detailed Readings Section
            Paragraph tableTitle = new Paragraph("Historial de Lecturas Detallado", sectionFont);
            tableTitle.setSpacingAfter(10);
            document.add(tableTitle);

            // Table setup
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 1.2f, 1.8f, 1.5f, 1.5f, 3.5f});
            table.setHeaderRows(1);

            // Headers
            String[] headers = {"Fecha", "Hora", "Glucosa (mg/dL)", "Tendencia", "Momento", "Notas"};
            Color headerBg = new Color(26, 54, 93);
            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                headerCell.setBackgroundColor(headerBg);
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                headerCell.setPadding(8);
                table.addCell(headerCell);
            }

            // Alternating colors
            Color alternateColor = new Color(247, 250, 252);
            Color baseColor = Color.WHITE;

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            boolean isAlternate = false;
            for (GlucoseReading r : readings) {
                Color rowBg = isAlternate ? alternateColor : baseColor;

                // Date (converted to user timezone)
                ZonedDateTime userDateTime = r.getFechaHora().withZoneSameInstant(userZone);
                PdfPCell dateCell = new PdfPCell(new Phrase(userDateTime.format(dateFormatter), bodyFont));
                dateCell.setBackgroundColor(rowBg);
                dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                dateCell.setPadding(6);
                table.addCell(dateCell);

                // Time (converted to user timezone)
                PdfPCell timeCell = new PdfPCell(new Phrase(userDateTime.format(timeFormatter), bodyFont));
                timeCell.setBackgroundColor(rowBg);
                timeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                timeCell.setPadding(6);
                table.addCell(timeCell);

                // Glucose (with warning color if critical)
                int val = r.getValorMgdl();
                Font valFont = bodyFont;
                if (val > 250 || val < 60) {
                    valFont = boldBodyFont;
                }
                PdfPCell valCell = new PdfPCell(new Phrase(val + " mg/dL", valFont));
                valCell.setBackgroundColor(rowBg);
                valCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                valCell.setPadding(6);
                table.addCell(valCell);

                // Tendency
                String tendency = r.getTendencia() != null ? r.getTendencia() : "Estable";
                PdfPCell tendencyCell = new PdfPCell(new Phrase(tendency, bodyFont));
                tendencyCell.setBackgroundColor(rowBg);
                tendencyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tendencyCell.setPadding(6);
                table.addCell(tendencyCell);

                // Momento
                String momento = r.getMomento() != null ? r.getMomento() : "N/A";
                PdfPCell momentoCell = new PdfPCell(new Phrase(momento, bodyFont));
                momentoCell.setBackgroundColor(rowBg);
                momentoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                momentoCell.setPadding(6);
                table.addCell(momentoCell);

                // Notas
                String notas = r.getNotas() != null ? r.getNotas() : "Sin notas";
                PdfPCell notasCell = new PdfPCell(new Phrase(notas, bodyFont));
                notasCell.setBackgroundColor(rowBg);
                notasCell.setPadding(6);
                table.addCell(notasCell);

                isAlternate = !isAlternate;
            }

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private Image createAgpChartImage(AgpDataResponse data) throws Exception {
        YIntervalSeries band90 = new YIntervalSeries("Rango 10-90%");
        YIntervalSeries band50 = new YIntervalSeries("Mediana y Rango 25-75%");

        List<AgpDataResponse.AgpPoint> mediana = data.getMediana();
        List<AgpDataResponse.AgpRangePoint> rango50 = data.getRango50();
        List<AgpDataResponse.AgpRangePoint> rango90 = data.getRango90();

        for (int i = 0; i < mediana.size(); i++) {
            double x = Double.parseDouble(mediana.get(i).getX().split(":")[0]); // "08:00" -> 8.0
            double med = mediana.get(i).getY();
            double[] r90 = rango90.get(i).getY();
            double[] r50 = rango50.get(i).getY();

            band90.add(x, med, r90[0], r90[1]);
            band50.add(x, med, r50[0], r50[1]);
        }

        YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
        dataset.addSeries(band90);
        dataset.addSeries(band50);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "",
                "Hora del día (0-23)",
                "Glucosa (mg/dL)",
                dataset
        );

        XYPlot plot = chart.getXYPlot();
        DeviationRenderer renderer = new DeviationRenderer(true, false);

        // Series 0: band90 (translucent background, no line)
        renderer.setSeriesStroke(0, new BasicStroke(0f));
        renderer.setSeriesPaint(0, new Color(0, 0, 0, 0)); 
        renderer.setSeriesFillPaint(0, new Color(46, 204, 113, 30));

        // Series 1: band50 + median (solid line + shaded band)
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        renderer.setSeriesPaint(1, new Color(39, 174, 96));
        renderer.setSeriesFillPaint(1, new Color(46, 204, 113, 80));

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setOutlineVisible(false);

        chart.setBackgroundPaint(Color.WHITE);

        BufferedImage bufferedImage = chart.createBufferedImage(800, 400);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);

        return Image.getInstance(baos.toByteArray());
    }
}
