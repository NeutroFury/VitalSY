import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { IonicModule, NavController, ToastController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { 
  personOutline, 
  settingsOutline, 
  fitnessOutline, 
  logOutOutline, 
  linkOutline,
  waterOutline,
  notificationsOutline,
  pulseOutline,
  scaleOutline,
  resizeOutline,
  thermometerOutline,
  alarmOutline,
  chevronDownOutline,
  cloudUploadOutline,
  calculatorOutline,
  flashOutline,
  timeOutline,
  chevronForwardOutline,
  refreshOutline
} from 'ionicons/icons';

import { HeaderComponent } from '../header/header.component';
import { UserService, UserProfile } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { LibreLinkUpService } from '../../services/librelinkup.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, IonicModule, HeaderComponent]
})
export class ProfileComponent implements OnInit {
  profileForm: FormGroup;
  isLoading = false;
  username: string = 'Usuario';
  isInsulinModalOpen = false;

  libreConfigured = false;
  libreEmail = '';
  librePassword = '';
  libreLastSync = '';
  isLibreLoading = false;
  isSyncing = false;

  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private navCtrl = inject(NavController);
  private toastCtrl = inject(ToastController);
  private libreService = inject(LibreLinkUpService);

  constructor() {
    addIcons({ 
      personOutline, 
      settingsOutline, 
      fitnessOutline, 
      logOutOutline, 
      linkOutline,
      waterOutline,
      notificationsOutline,
      pulseOutline,
      scaleOutline,
      resizeOutline,
      thermometerOutline,
      alarmOutline,
      chevronDownOutline,
      cloudUploadOutline,
      calculatorOutline,
      flashOutline,
      timeOutline,
      chevronForwardOutline,
      refreshOutline
    });
    
    this.username = this.authService.getUsername();
    
    this.profileForm = this.fb.group({
      nombre: ['', [Validators.required]],
      pesoActual: [null, [Validators.required, Validators.min(1)]],
      altura: [null, [Validators.required, Validators.min(1)]],
      tipoInsulina: ['Humalog', [Validators.required]],
      ratioIc: [10, [Validators.required, Validators.min(0.1)]],
      factorIs: [40, [Validators.required, Validators.min(1)]],
      alertasGlucosa: [true],
      recordatorioComidas: [false]
    });
  }

  ngOnInit() {
    this.loadProfile();
    this.loadLibreStatus();
  }

  loadProfile() {
    this.isLoading = true;
    this.userService.getUserProfile().subscribe({
      next: (profile) => {
        this.profileForm.patchValue(profile);
        if (profile.nombre) {
          localStorage.setItem('username', profile.nombre);
          this.username = profile.nombre;
        }
        if (profile.rangoGlucosaMin !== undefined) {
          localStorage.setItem('rangoGlucosaMin', String(profile.rangoGlucosaMin));
        }
        if (profile.rangoGlucosaMax !== undefined) {
          localStorage.setItem('rangoGlucosaMax', String(profile.rangoGlucosaMax));
        }
        this.isLoading = false;
      },
      error: () => {
        this.showToast('Error al conectar con la central VitalSY', 'danger');
        this.isLoading = false;
      }
    });
  }

  selectInsulin(value: string) {
    this.profileForm.get('tipoInsulina')?.setValue(value);
    this.isInsulinModalOpen = false;
    this.saveProfile();
  }

  recordatorioComidasToggle() {
    const currentValue = this.profileForm.get('recordatorioComidas')?.value;
    this.profileForm.get('recordatorioComidas')?.setValue(!currentValue);
    this.saveProfile();
  }

  saveProfile() {
    if (this.profileForm.valid) {
      this.isLoading = true;
      this.userService.updateUserProfile(this.profileForm.getRawValue()).subscribe({
        next: (updatedProfile) => {
          this.showToast('Configuración Clínica Actualizada', 'success');
          if (updatedProfile.nombre) {
            localStorage.setItem('username', updatedProfile.nombre);
            this.username = updatedProfile.nombre;
          }
          if (updatedProfile.rangoGlucosaMin !== undefined) {
            localStorage.setItem('rangoGlucosaMin', String(updatedProfile.rangoGlucosaMin));
          }
          if (updatedProfile.rangoGlucosaMax !== undefined) {
            localStorage.setItem('rangoGlucosaMax', String(updatedProfile.rangoGlucosaMax));
          }
          this.isLoading = false;
        },
        error: () => {
          this.showToast('Fallo en la sincronización de datos', 'danger');
          this.isLoading = false;
        }
      });
    } else {
      this.showToast('Verifica los valores clínicos ingresados', 'warning');
    }
  }

  async showToast(message: string, color: string) {
    const toast = await this.toastCtrl.create({
      message,
      duration: 2000,
      color,
      position: 'bottom',
      cssClass: 'vitalsy-toast'
    });
    await toast.present();
  }

  logout() {
    this.authService.logout();
    this.navCtrl.navigateRoot('/login', { animated: true, animationDirection: 'back' });
  }

  loadLibreStatus() {
    this.isLibreLoading = true;
    this.libreService.getStatus().subscribe({
      next: (status) => {
        this.libreConfigured = status.configurado;
        if (status.configurado) {
          this.libreEmail = status.email || '';
          this.libreLastSync = status.ultimoSync || 'Nunca';
        } else {
          this.libreEmail = '';
          this.libreLastSync = '';
        }
        this.librePassword = '';
        this.isLibreLoading = false;
      },
      error: () => {
        this.isLibreLoading = false;
      }
    });
  }

  connectLibreLinkUp() {
    if (!this.libreEmail || !this.librePassword) {
      this.showToast('Ingresa tu correo y contraseña de LibreLinkUp', 'warning');
      return;
    }
    this.isLibreLoading = true;
    this.libreService.setup({ email: this.libreEmail, password: this.librePassword }).subscribe({
      next: (res) => {
        this.showToast(res.mensaje || 'Vínculo establecido con éxito', 'success');
        this.loadLibreStatus();
      },
      error: (err) => {
        console.error(err);
        this.showToast('Error al vincular. Verifica tus credenciales.', 'danger');
        this.isLibreLoading = false;
      }
    });
  }

  disconnectLibreLinkUp() {
    this.isLibreLoading = true;
    this.libreService.disconnect().subscribe({
      next: (res) => {
        this.showToast(res.mensaje || 'Desconectado de LibreLinkUp', 'success');
        this.loadLibreStatus();
      },
      error: (err) => {
        console.error(err);
        this.showToast('Error al desconectar la cuenta', 'danger');
        this.isLibreLoading = false;
      }
    });
  }

  syncLibreLinkUp() {
    this.isSyncing = true;
    this.libreService.forceSync().subscribe({
      next: (res) => {
        this.showToast(`${res.mensaje} (${res.nuevosRegistros} lecturas nuevas)`, 'success');
        this.loadLibreStatus();
        this.isSyncing = false;
      },
      error: (err) => {
        console.error(err);
        this.showToast('Error sincronizando lecturas de Abbott', 'danger');
        this.isSyncing = false;
      }
    });
  }
}
