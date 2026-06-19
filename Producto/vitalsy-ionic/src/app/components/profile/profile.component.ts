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
import { AlertSettingsComponent } from '../alert-settings/alert-settings.component';
import { RecordatoriosComponent } from '../recordatorios/recordatorios.component';
import { UserService, UserProfile } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { LibreLinkUpService } from '../../services/librelinkup.service';
import { CognitivoService } from '../../services/cognitivo.service';
import { PushNotificationsService } from '../../services/push-notifications.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, IonicModule, HeaderComponent, AlertSettingsComponent, RecordatoriosComponent]
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
  private pushNotificationsService = inject(PushNotificationsService);

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
      glicemiaObjetivo: [100, [Validators.required, Validators.min(50)]],
      alertasGlucosa: [true]
    });
  }

  ngOnInit() {
    this.loadProfile();
    this.loadLibreStatus();
    // Inicializar push notifications al entrar al perfil
    // (si el usuario aún no ha otorgado permisos, se les pedirá aquí)
    this.pushNotificationsService.initialize();
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
          glicemiaObjetivo: profile.glicemiaObjetivo || this.profileForm.get('glicemiaObjetivo')?.value || 100,
          alertasGlucosa: profile.alertasGlucosa !== null && profile.alertasGlucosa !== undefined ? profile.alertasGlucosa : true,
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
    this.isScanning = true;
    this.showToast('Inyectando Pauta Médica Completa...', 'tertiary');

    const mockData = [
      // === MATRIZ DESAYUNO ===
      {"momentoDia": "DESAYUNO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 0, "dosisInsulina": 0},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 6, "dosisInsulina": 1},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 12, "dosisInsulina": 2},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 18, "dosisInsulina": 3},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 24, "dosisInsulina": 4},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 30, "dosisInsulina": 5},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 36, "dosisInsulina": 6},
      
      {"momentoDia": "DESAYUNO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 0, "dosisInsulina": 1},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 6, "dosisInsulina": 2},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 12, "dosisInsulina": 3},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 18, "dosisInsulina": 4},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 24, "dosisInsulina": 5},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 30, "dosisInsulina": 6},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 36, "dosisInsulina": 7},

      {"momentoDia": "DESAYUNO", "glicemiaMin": 131, "glicemiaMax": 160, "carbohidratosGr": 12, "dosisInsulina": 4},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 131, "glicemiaMax": 160, "carbohidratosGr": 18, "dosisInsulina": 5},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 161, "glicemiaMax": 190, "carbohidratosGr": 12, "dosisInsulina": 5},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 161, "glicemiaMax": 190, "carbohidratosGr": 18, "dosisInsulina": 6},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 191, "glicemiaMax": 220, "carbohidratosGr": 12, "dosisInsulina": 6},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 191, "glicemiaMax": 220, "carbohidratosGr": 18, "dosisInsulina": 7},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 221, "glicemiaMax": 250, "carbohidratosGr": 12, "dosisInsulina": 7},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 221, "glicemiaMax": 250, "carbohidratosGr": 18, "dosisInsulina": 8},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 251, "glicemiaMax": 280, "carbohidratosGr": 12, "dosisInsulina": 8},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 251, "glicemiaMax": 280, "carbohidratosGr": 18, "dosisInsulina": 9},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 281, "glicemiaMax": 310, "carbohidratosGr": 12, "dosisInsulina": 9},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 281, "glicemiaMax": 310, "carbohidratosGr": 18, "dosisInsulina": 10},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 311, "glicemiaMax": 340, "carbohidratosGr": 12, "dosisInsulina": 10},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 311, "glicemiaMax": 340, "carbohidratosGr": 18, "dosisInsulina": 11},

      {"momentoDia": "DESAYUNO", "glicemiaMin": 341, "glicemiaMax": 999, "carbohidratosGr": 12, "dosisInsulina": 11},
      {"momentoDia": "DESAYUNO", "glicemiaMin": 341, "glicemiaMax": 999, "carbohidratosGr": 18, "dosisInsulina": 12},
      // === MATRIZ ALMUERZO ===
      {"momentoDia": "ALMUERZO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 0, "dosisInsulina": 0},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 10, "dosisInsulina": 2},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 20, "dosisInsulina": 4},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 30, "dosisInsulina": 6},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 40, "dosisInsulina": 8},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 50, "dosisInsulina": 10},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 60, "dosisInsulina": 12},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 70, "dosisInsulina": 14},

      {"momentoDia": "ALMUERZO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 0, "dosisInsulina": 1},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 10, "dosisInsulina": 3},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 20, "dosisInsulina": 5},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 30, "dosisInsulina": 7},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 40, "dosisInsulina": 9},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 50, "dosisInsulina": 11},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 60, "dosisInsulina": 13},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 70, "dosisInsulina": 15},

      {"momentoDia": "ALMUERZO", "glicemiaMin": 131, "glicemiaMax": 160, "carbohidratosGr": 30, "dosisInsulina": 8},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 161, "glicemiaMax": 190, "carbohidratosGr": 30, "dosisInsulina": 9},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 191, "glicemiaMax": 220, "carbohidratosGr": 30, "dosisInsulina": 10},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 221, "glicemiaMax": 250, "carbohidratosGr": 30, "dosisInsulina": 11},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 251, "glicemiaMax": 280, "carbohidratosGr": 30, "dosisInsulina": 12},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 281, "glicemiaMax": 310, "carbohidratosGr": 30, "dosisInsulina": 13},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 311, "glicemiaMax": 340, "carbohidratosGr": 30, "dosisInsulina": 14},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 341, "glicemiaMax": 999, "carbohidratosGr": 20, "dosisInsulina": 13},
      {"momentoDia": "ALMUERZO", "glicemiaMin": 341, "glicemiaMax": 999, "carbohidratosGr": 30, "dosisInsulina": 15},

      // === MATRIZ ONCE/CENA (SIN EJERCICIO) ===
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 0, "dosisInsulina": 0},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 15, "dosisInsulina": 2.5},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 30, "dosisInsulina": 4.5},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 45, "dosisInsulina": 6.5},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 60, "dosisInsulina": 8.5},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 75, "dosisInsulina": 10.5},
      
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 0, "dosisInsulina": 1},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 15, "dosisInsulina": 3.5},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 30, "dosisInsulina": 5.5},
      
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 131, "glicemiaMax": 160, "carbohidratosGr": 15, "dosisInsulina": 4.5},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 131, "glicemiaMax": 160, "carbohidratosGr": 30, "dosisInsulina": 6.0},

      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 161, "glicemiaMax": 190, "carbohidratosGr": 15, "dosisInsulina": 4.5},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 161, "glicemiaMax": 190, "carbohidratosGr": 30, "dosisInsulina": 6.0},

      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 191, "glicemiaMax": 220, "carbohidratosGr": 15, "dosisInsulina": 4.5},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 191, "glicemiaMax": 220, "carbohidratosGr": 30, "dosisInsulina": 6.0},

      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 221, "glicemiaMax": 250, "carbohidratosGr": 15, "dosisInsulina": 4.5},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 221, "glicemiaMax": 250, "carbohidratosGr": 30, "dosisInsulina": 6.0},

      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 251, "glicemiaMax": 280, "carbohidratosGr": 15, "dosisInsulina": 4.5},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 251, "glicemiaMax": 280, "carbohidratosGr": 30, "dosisInsulina": 6.0},

      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 281, "glicemiaMax": 310, "carbohidratosGr": 15, "dosisInsulina": 5.0},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 281, "glicemiaMax": 310, "carbohidratosGr": 30, "dosisInsulina": 6.5},

      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 311, "glicemiaMax": 340, "carbohidratosGr": 15, "dosisInsulina": 5.5},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 311, "glicemiaMax": 340, "carbohidratosGr": 30, "dosisInsulina": 7.0},

      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 341, "glicemiaMax": 999, "carbohidratosGr": 15, "dosisInsulina": 12.0},
      {"momentoDia": "ONCE_CENA_SIN_EJERCICIO", "glicemiaMin": 341, "glicemiaMax": 999, "carbohidratosGr": 30, "dosisInsulina": 13.5},

      // === MATRIZ ONCE/CENA (CON EJERCICIO) ===
      {"momentoDia": "ONCE_CENA_CON_EJERCICIO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 0, "dosisInsulina": 0},
      {"momentoDia": "ONCE_CENA_CON_EJERCICIO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 15, "dosisInsulina": 1.5},
      {"momentoDia": "ONCE_CENA_CON_EJERCICIO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 30, "dosisInsulina": 3.0},
      {"momentoDia": "ONCE_CENA_CON_EJERCICIO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 45, "dosisInsulina": 4.5},
      {"momentoDia": "ONCE_CENA_CON_EJERCICIO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 60, "dosisInsulina": 6.0},
      {"momentoDia": "ONCE_CENA_CON_EJERCICIO", "glicemiaMin": 0, "glicemiaMax": 100, "carbohidratosGr": 75, "dosisInsulina": 7.5},
      
      {"momentoDia": "ONCE_CENA_CON_EJERCICIO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 0, "dosisInsulina": 1},
      {"momentoDia": "ONCE_CENA_CON_EJERCICIO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 15, "dosisInsulina": 2.5},
      {"momentoDia": "ONCE_CENA_CON_EJERCICIO", "glicemiaMin": 101, "glicemiaMax": 130, "carbohidratosGr": 30, "dosisInsulina": 4.0},
      {"momentoDia": "ONCE_CENA_CON_EJERCICIO", "glicemiaMin": 131, "glicemiaMax": 160, "carbohidratosGr": 15, "dosisInsulina": 3.5},
      {"momentoDia": "ONCE_CENA_CON_EJERCICIO", "glicemiaMin": 341, "glicemiaMax": 999, "carbohidratosGr": 30, "dosisInsulina": 12.0}
    ];

    this.cognitivoService.inyectarPautaMock(mockData).subscribe({
      next: (res) => {
        this.isScanning = false;
        this.showToast(res.mensaje || '¡Pauta Médica Completa Inyectada en Supabase!', 'success');
      },
      error: (err) => {
        console.error(err);
        this.isScanning = false;
        this.showToast('Error al inyectar la pauta médica', 'danger');
      }
    });

    const element = event.currentTarget as HTMLInputElement;
    if (element) {
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
