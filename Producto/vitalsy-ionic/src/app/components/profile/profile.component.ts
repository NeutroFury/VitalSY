import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { take } from 'rxjs/operators';
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
  refreshOutline,
  closeOutline,
  checkmarkOutline,
  cameraOutline
} from 'ionicons/icons';

import { HeaderComponent } from '../header/header.component';
import { UserService, UserProfile } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { LibreLinkUpService } from '../../services/librelinkup.service';
import { CognitivoService } from '../../services/cognitivo.service';

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
  currentInsulinTarget: 'lenta' | 'rapida' = 'lenta';
  customInsulinName: string = '';

  libreConfigured = false;
  libreEmail = '';
  librePassword = '';
  libreLastSync = '';
  isLibreLoading = false;
  isSyncing = false;
  isScanning = false;

  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private navCtrl = inject(NavController);
  private toastCtrl = inject(ToastController);
  private libreService = inject(LibreLinkUpService);
  private cognitivoService = inject(CognitivoService);

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
      refreshOutline,
      closeOutline,
      checkmarkOutline,
      cameraOutline
    });

    this.username = this.authService.getUsername();

    this.profileForm = this.fb.group({
      nombre: ['', [Validators.required]],
      pesoActual: [null, [Validators.required, Validators.min(1)]],
      altura: [null, [Validators.required, Validators.min(1)]],
      insulinaLenta: ['Tresiba (Degludec)', [Validators.required]],
      insulinaRapida: ['Humalog (Lispro)', [Validators.required]],
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
    this.userService.getUserProfile().pipe(take(1)).subscribe({
      next: (profile) => {
        this.profileForm.patchValue({
          ...profile,
          pesoActual: profile.pesoActual || this.profileForm.get('pesoActual')?.value,
          altura: profile.altura || this.profileForm.get('altura')?.value,
          ratioIc: profile.ratioIc || this.profileForm.get('ratioIc')?.value || 10,
          factorIs: profile.factorIs || this.profileForm.get('factorIs')?.value || 40,
          alertasGlucosa: profile.alertasGlucosa !== null && profile.alertasGlucosa !== undefined ? profile.alertasGlucosa : true,
          recordatorioComidas: profile.recordatorioComidas !== null && profile.recordatorioComidas !== undefined ? profile.recordatorioComidas : false,
          // Evitar que null sobreescriba los valores con vacíos si el usuario ya tenía uno seleccionado
          insulinaLenta: profile.insulinaLenta || this.profileForm.get('insulinaLenta')?.value || 'Tresiba (Degludec)',
          insulinaRapida: profile.insulinaRapida || this.profileForm.get('insulinaRapida')?.value || 'Humalog (Lispro)'
        });

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

  openInsulinModal(target: 'lenta' | 'rapida') {
    this.currentInsulinTarget = target;
    this.customInsulinName = '';
    this.isInsulinModalOpen = true;
  }

  selectInsulin(value: string) {
    const control = this.currentInsulinTarget === 'lenta' ? 'insulinaLenta' : 'insulinaRapida';
    this.profileForm.get(control)?.setValue(value);
    this.isInsulinModalOpen = false;
    this.customInsulinName = '';
  }

  selectCustomInsulin() {
    if (this.customInsulinName.trim()) {
      this.selectInsulin(this.customInsulinName.trim());
    }
  }

  recordatorioComidasToggle() {
    const currentValue = this.profileForm.get('recordatorioComidas')?.value;
    this.profileForm.get('recordatorioComidas')?.setValue(!currentValue);
  }

  saveProfile() {
    if (this.profileForm.valid) {
      this.isLoading = true;
      const data = this.profileForm.getRawValue();
      console.log('DATOS A GUARDAR:', JSON.stringify(data));

      // Enviamos el objeto de manera one-shot y sin llamar de vuelta a loadProfile()
      this.userService.updateUserProfile(data).pipe(take(1)).subscribe({
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

  onFileSelected(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;
    if (fileList && fileList.length > 0) {
      const file = fileList[0];
      this.isScanning = true;
      this.showToast('Analizando pauta médica con Inteligencia Artificial...', 'tertiary');

      this.cognitivoService.subirPautaMedica(file).subscribe({
        next: (res) => {
          this.isScanning = false;
          this.showToast(`${res.mensaje} (${res.reglasExtraidas} reglas guardadas)`, 'success');
        },
        error: (err) => {
          console.error(err);
          this.isScanning = false;
          this.showToast('Error al procesar la pauta médica', 'danger');
        }
      });

      // Reiniciar el input
      element.value = '';
    }
  }

  triggerCameraInput() {
    const input = document.getElementById('cameraInput') as HTMLInputElement;
    if (input) {
      input.click();
    }
  }
}
