import { Component, EventEmitter, inject, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule, NavController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { radioOutline, linkOutline, closeOutline } from 'ionicons/icons';

@Component({
  selector: 'app-sensor-sync-cta',
  templateUrl: './sensor-sync-cta.component.html',
  standalone: true,
  imports: [CommonModule, IonicModule]
})
export class SensorSyncCtaComponent {

  @Output() dismissed = new EventEmitter<void>();

  private navCtrl = inject(NavController);

  constructor() {
    addIcons({ radioOutline, linkOutline, closeOutline });
  }

  goToProfile() {
    this.navCtrl.navigateForward('/profile');
  }

  dismiss() {
    this.dismissed.emit();
  }
}
