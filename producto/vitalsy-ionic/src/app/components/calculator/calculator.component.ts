import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { calculatorOutline, radioOutline, informationCircleOutline } from 'ionicons/icons';

import { HeaderComponent } from '../header/header.component';

@Component({
  selector: 'app-calculator',
  templateUrl: './calculator.component.html',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, HeaderComponent]
})
export class CalculatorComponent {
  currentGlucose: string = '180';
  targetGlucose: string = '100';
  carbsToConsume: string = '45';
  icRatio: string = '10'; // 1 unidad por cada 10g de carbs
  isSensitivy: string = '40'; // 1 unidad baja 40mg/dL
  
  isSyncing: boolean = false;

  constructor() {
    addIcons({ calculatorOutline, radioOutline, informationCircleOutline });
  }

  get correctionDose(): string {
    const g = parseFloat(this.currentGlucose) || 0;
    const tg = parseFloat(this.targetGlucose) || 0;
    const is = parseFloat(this.isSensitivy) || 1;
    return Math.max(0, (g - tg) / is).toFixed(1);
  }

  get carbohydrateDose(): string {
    const c = parseFloat(this.carbsToConsume) || 0;
    const ic = parseFloat(this.icRatio) || 1;
    return (c / ic).toFixed(1);
  }

  get totalDose(): string {
    return (parseFloat(this.correctionDose) + parseFloat(this.carbohydrateDose)).toFixed(1);
  }

  handleSyncSensor() {
    this.isSyncing = true;
    // Simular sincronización NFC/Bluetooth
    setTimeout(() => {
      const mockSensorValue = Math.floor(Math.random() * (220 - 90 + 1) + 90).toString();
      this.currentGlucose = mockSensorValue;
      this.isSyncing = false;
    }, 2000);
  }

  confirmDose() {
    console.log('Dosis confirmada:', this.totalDose);
    // Lógica para guardar o regresar
  }
}
