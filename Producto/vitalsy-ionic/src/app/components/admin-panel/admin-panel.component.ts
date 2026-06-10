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
  closeOutline
} from 'ionicons/icons';
import { AdminService, PacienteAdmin, AdminStats } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';
import { FormsModule } from '@angular/forms';

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
      personOutline, mailOutline, searchOutline, closeOutline
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
}
