import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule, ToastController } from '@ionic/angular';
import { ActivatedRoute, Router } from '@angular/router';
import { addIcons } from 'ionicons';
import {
  arrowBackOutline,
  waterOutline,
  flashOutline,
  documentTextOutline,
  pulseOutline,
  medicalOutline
} from 'ionicons/icons';
import { AdminService, PacienteAdmin } from '../../services/admin.service';
import { BaseChartDirective, provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { ChartConfiguration, ChartOptions, ChartType } from 'chart.js';

@Component({
  selector: 'app-paciente-detalle',
  templateUrl: './paciente-detalle.component.html',
  styleUrls: ['./paciente-detalle.component.scss'],
  standalone: true,
  imports: [CommonModule, IonicModule, BaseChartDirective],
  providers: [provideCharts(withDefaultRegisterables())]
})
export class PacienteDetalleComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private adminService = inject(AdminService);
  private toastCtrl = inject(ToastController);

  paciente: PacienteAdmin | null = null;
  isLoading = true;
  hasRegistros = true;

  public lineChartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Glucosa (mg/dL)',
        fill: false,
        tension: 0.4,
        borderColor: '#d4ff00',
        backgroundColor: '#d4ff00',
        pointBackgroundColor: '#d4ff00',
        pointBorderColor: '#fff',
        pointHoverBackgroundColor: '#fff',
        pointHoverBorderColor: '#d4ff00',
        pointRadius: 4,
        pointHoverRadius: 6,
        borderWidth: 2
      }
    ]
  };
  
  public lineChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: 'rgba(5, 5, 5, 0.8)',
        titleColor: '#fff',
        bodyColor: '#d4ff00',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderWidth: 1,
        padding: 10,
        displayColors: false
      }
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { color: '#8b8b93', font: { size: 10 } }
      },
      y: {
        grid: { color: 'rgba(255, 255, 255, 0.05)' },
        ticks: { color: '#8b8b93', font: { size: 10 } },
        beginAtZero: true
      }
    }
  };

  constructor() {
    addIcons({
      arrowBackOutline, waterOutline, flashOutline,
      documentTextOutline, pulseOutline, medicalOutline
    });
  }

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.loadPaciente(parseInt(idParam, 10));
    } else {
      this.router.navigate(['/admin-panel']);
    }
  }

  loadPaciente(id: number) {
    this.isLoading = true;
    this.adminService.getPacienteById(id).subscribe({
      next: (data) => {
        this.paciente = data;
        this.isLoading = false;
        this.loadHistorial(id);
      },
      error: async (err) => {
        console.error('Error cargando paciente', err);
        const toast = await this.toastCtrl.create({
          message: 'Error al cargar los datos del paciente.',
          duration: 3000, color: 'danger', position: 'top'
        });
        toast.present();
        this.isLoading = false;
        this.router.navigate(['/admin-panel']);
      }
    });
  }

  loadHistorial(id: number) {
    this.adminService.getUltimosRegistrosGlucosa(id).subscribe({
      next: (registros) => {
        if (!registros || registros.length === 0) {
          this.hasRegistros = false;
          return;
        }
        
        this.hasRegistros = true;
        const labels: string[] = [];
        const data: number[] = [];
        
        // Asumiendo que vienen ordenados DESC, los invertimos para mostrarlos de forma ASC en la gráfica
        const registrosAsc = [...registros].reverse();

        registrosAsc.forEach(r => {
          // Formateamos la fecha a dd/MM HH:mm
          const fecha = new Date(r.fechaHora);
          const dia = fecha.getDate().toString().padStart(2, '0');
          const mes = (fecha.getMonth() + 1).toString().padStart(2, '0');
          const hora = fecha.getHours().toString().padStart(2, '0');
          const min = fecha.getMinutes().toString().padStart(2, '0');
          labels.push(`${dia}/${mes} ${hora}:${min}`);
          data.push(r.valorMgdl);
        });

        this.lineChartData = {
          labels: labels,
          datasets: [{ ...this.lineChartData.datasets[0], data: data }]
        };
      },
      error: (err) => {
        console.error('Error cargando historial de glucosa', err);
        this.hasRegistros = false;
      }
    });
  }

  getInitials(nombre: string): string {
    if (!nombre) return '';
    return nombre
      .split(' ')
      .slice(0, 2)
      .map(n => n[0]?.toUpperCase() ?? '')
      .join('');
  }

  formatInsulin(value: string | null | undefined): string {
    return value && value !== 'Ninguna' ? value : '—';
  }
}
