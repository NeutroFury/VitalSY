import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { Router } from '@angular/router';
import { addIcons } from 'ionicons';
import {
  peopleOutline,
  statsChartOutline,
  logOutOutline,
  refreshOutline,
  shieldCheckmarkOutline,
  waterOutline,
  flashOutline,
  timeOutline,
  personOutline,
  mailOutline,
  searchOutline,
  closeOutline,
  banOutline,
  checkmarkCircleOutline,
  eyeOutline
} from 'ionicons/icons';
import { AdminService, PacienteAdmin, AdminStats } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';
import { FormsModule } from '@angular/forms';
import { ToastController, AlertController } from '@ionic/angular';

@Component({
  selector: 'app-admin-panel',
  templateUrl: './admin-panel.component.html',
  styleUrls: ['./admin-panel.component.scss'],
  standalone: true,
  imports: [CommonModule, IonicModule, FormsModule]
})
export class AdminPanelComponent implements OnInit {
  private adminService = inject(AdminService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private toastCtrl = inject(ToastController);
  private alertCtrl = inject(AlertController);

  pacientes: PacienteAdmin[] = [];
  filteredPacientes: PacienteAdmin[] = [];
  stats: AdminStats | null = null;
  isLoading = true;
  searchQuery = '';
  activeTab: 'pacientes' | 'stats' = 'pacientes';
  adminNombre = this.authService.getUsername();

  constructor() {
    addIcons({
      peopleOutline, statsChartOutline, logOutOutline, refreshOutline,
      shieldCheckmarkOutline, waterOutline, flashOutline, timeOutline,
      personOutline, mailOutline, searchOutline, closeOutline,
      banOutline, checkmarkCircleOutline, eyeOutline
    });
  }

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.isLoading = true;
    this.adminService.getPacientes().subscribe({
      next: (data) => {
        this.pacientes = data;
        this.filteredPacientes = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error cargando pacientes', err);
        this.isLoading = false;
      }
    });

    this.adminService.getStats().subscribe({
      next: (s) => this.stats = s,
      error: (err) => console.error('Error cargando stats', err)
    });
  }

  onSearch(event: any) {
    const q = (event.target?.value ?? this.searchQuery).toLowerCase().trim();
    this.filteredPacientes = this.pacientes.filter(p =>
      p.nombre.toLowerCase().includes(q) ||
      p.email.toLowerCase().includes(q)
    );
  }

  clearSearch() {
    this.searchQuery = '';
    this.filteredPacientes = [...this.pacientes];
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  formatInsulin(value: string | null | undefined): string {
    return value && value !== 'Ninguna' ? value : '—';
  }

  getInitials(nombre: string): string {
    return nombre
      .split(' ')
      .slice(0, 2)
      .map(n => n[0]?.toUpperCase() ?? '')
      .join('');
  }

  goToDetalle(id: number) {
    this.router.navigate(['/admin/paciente', id]);
  }

  async onToggleStatus(paciente: PacienteAdmin) {
    const alert = await this.alertCtrl.create({
      header: 'Confirmar',
      message: `¿Estás seguro que deseas ${paciente.activo ? 'suspender' : 'activar'} a ${paciente.nombre}?`,
      buttons: [
        { text: 'Cancelar', role: 'cancel', cssClass: 'text-zinc-400' },
        { 
          text: 'Sí, continuar', 
          handler: () => {
            this.adminService.toggleStatus(paciente.id).subscribe({
              next: async (res) => {
                paciente.activo = !paciente.activo;
                const toast = await this.toastCtrl.create({
                  message: res.mensaje || 'Estado actualizado.',
                  duration: 3000, color: 'success', position: 'top'
                });
                toast.present();
              },
              error: async (err) => {
                const toast = await this.toastCtrl.create({
                  message: err.error?.message || 'Error al actualizar el estado.',
                  duration: 3000, color: 'danger', position: 'top'
                });
                toast.present();
              }
            });
          }
        }
      ]
    });
    await alert.present();
  }

  async onTriggerPasswordReset(paciente: PacienteAdmin) {
    const alert = await this.alertCtrl.create({
      header: 'Recuperación de Contraseña',
      message: `¿Deseas enviar un enlace de recuperación al correo ${paciente.email}?`,
      buttons: [
        { text: 'Cancelar', role: 'cancel', cssClass: 'text-zinc-400' },
        { 
          text: 'Enviar Correo', 
          handler: () => {
            this.adminService.triggerPasswordReset(paciente.id).subscribe({
              next: async (res) => {
                const toast = await this.toastCtrl.create({
                  message: res.mensaje || 'Correo enviado al paciente.',
                  duration: 4000, color: 'success', position: 'top'
                });
                toast.present();
              },
              error: async (err) => {
                const toast = await this.toastCtrl.create({
                  message: err.error?.message || 'Error al enviar el correo.',
                  duration: 3000, color: 'danger', position: 'top'
                });
                toast.present();
              }
            });
          }
        }
      ]
    });
    await alert.present();
  }
}
