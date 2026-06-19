import { Component, OnInit, inject, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
  ValidationErrors
} from '@angular/forms';
import { IonicModule, ToastController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { notificationsOutline, pulseOutline, warningOutline } from 'ionicons/icons';
import { take } from 'rxjs/operators';
import { UserService } from '../../services/user.service';

/**
 * Validador cruzado: el mínimo siempre debe ser menor que el máximo.
 */
function rangoGlucosaValidator(group: AbstractControl): ValidationErrors | null {
  const min = group.get('rangoGlucosaMin')?.value;
  const max = group.get('rangoGlucosaMax')?.value;
  if (min !== null && max !== null && min >= max) {
    return { rangeInvalid: true };
  }
  return null;
}

/**
 * Componente de configuración de alertas de glucosa.
 * Permite al paciente ajustar sus umbrales de hipoglicemia e hiperglicemia
 * y activar/desactivar las notificaciones push.
 *
 * Uso en profile.component.html:
 *   <app-alert-settings (alertasSaved)="onAlertasSaved()"></app-alert-settings>
 */
@Component({
  selector: 'app-alert-settings',
  templateUrl: './alert-settings.component.html',
  styleUrls: ['./alert-settings.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, IonicModule]
})
export class AlertSettingsComponent implements OnInit {
  /** Emitido cuando el usuario guarda los cambios exitosamente */
  @Output() alertasSaved = new EventEmitter<void>();

  alertForm: FormGroup;
  isSaving = false;

  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private toastCtrl = inject(ToastController);

  constructor() {
    addIcons({ notificationsOutline, pulseOutline, warningOutline });

    this.alertForm = this.fb.group(
      {
        alertasGlucosa: [true],
        rangoGlucosaMin: [70, [Validators.required, Validators.min(50), Validators.max(120)]],
        rangoGlucosaMax: [180, [Validators.required, Validators.min(120), Validators.max(300)]]
      },
      { validators: rangoGlucosaValidator }
    );
  }

  ngOnInit(): void {
    this.cargarValoresActuales();
  }

  /** Carga los valores actuales del perfil del usuario */
  private cargarValoresActuales(): void {
    this.userService.getUserProfile().pipe(take(1)).subscribe({
      next: (profile) => {
        this.alertForm.patchValue({
          alertasGlucosa: profile.alertasGlucosa ?? true,
          rangoGlucosaMin: profile.rangoGlucosaMin ?? 70,
          rangoGlucosaMax: profile.rangoGlucosaMax ?? 180
        });
      },
      error: () => this.showToast('No se pudieron cargar los umbrales actuales', 'warning')
    });
  }

  /**
   * Guarda los umbrales de alerta en el backend.
   * Usa el endpoint existente PUT /api/v1/usuarios/parametros-clinicos
   * para los rangos, y PUT /api/v1/usuarios/perfil para la preferencia de alertas.
   */
  guardarAlertas(): void {
    if (this.alertForm.invalid) {
      this.showToast('Verifica los valores de umbral ingresados', 'warning');
      return;
    }

    this.isSaving = true;
    const { alertasGlucosa, rangoGlucosaMin, rangoGlucosaMax } = this.alertForm.getRawValue();

    // Guardar preferencia de alertas y rangos en una sola llamada al perfil
    this.userService.updateUserProfile({
      alertasGlucosa,
      rangoGlucosaMin,
      rangoGlucosaMax
    }).pipe(take(1)).subscribe({
      next: () => {
        this.showToast('✅ Alertas de glucosa actualizadas', 'success');
        this.alertasSaved.emit();
        this.isSaving = false;
      },
      error: () => {
        this.showToast('Error al guardar la configuración de alertas', 'danger');
        this.isSaving = false;
      }
    });
  }

  /** Formatea el pin del slider con unidad mg/dL */
  formatPin(value: number): string {
    return `${value}`;
  }

  private async showToast(message: string, color: string): Promise<void> {
    const toast = await this.toastCtrl.create({
      message,
      duration: 2500,
      color,
      position: 'bottom',
      cssClass: 'vitalsy-toast'
    });
    await toast.present();
  }
}
