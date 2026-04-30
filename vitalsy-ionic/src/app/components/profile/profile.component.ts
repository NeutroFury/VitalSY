import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
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
  chevronForwardOutline
} from 'ionicons/icons';

import { HeaderComponent } from '../header/header.component';
import { UserService, UserProfile } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, IonicModule, HeaderComponent]
})
export class ProfileComponent implements OnInit {
  profileForm: FormGroup;
  isLoading = false;
  username: string = 'Usuario';
  isInsulinModalOpen = false;

  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private navCtrl = inject(NavController);
  private toastCtrl = inject(ToastController);

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
      chevronForwardOutline
    });
    
    this.username = this.authService.getUsername();
    
    this.profileForm = this.fb.group({
      nombre: [{ value: '', disabled: true }],
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
  }

  loadProfile() {
    this.isLoading = true;
    this.userService.getUserProfile().subscribe({
      next: (profile) => {
        this.profileForm.patchValue(profile);
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
        next: () => {
          this.showToast('Configuración Clínica Actualizada', 'success');
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
}
