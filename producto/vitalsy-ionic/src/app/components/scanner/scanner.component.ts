import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { radioOutline } from 'ionicons/icons';

@Component({
  selector: 'app-scanner',
  templateUrl: './scanner.component.html',
  standalone: true,
  imports: [CommonModule, IonicModule]
})
export class ScannerComponent {
  constructor() {
    addIcons({ radioOutline });
  }
}
