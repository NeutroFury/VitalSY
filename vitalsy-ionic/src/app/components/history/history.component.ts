import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { waterOutline, nutritionOutline, arrowForwardOutline, downloadOutline } from 'ionicons/icons';

import { HeaderComponent } from '../header/header.component';

@Component({
  selector: 'app-history',
  templateUrl: './history.component.html',
  standalone: true,
  imports: [CommonModule, IonicModule, HeaderComponent]
})
export class HistoryComponent {
  logs = [
    { id: 1, type: 'Desayuno', glucose: 110, carbs: 45, time: '08:30 AM', date: '20 Abril, 2024', note: 'Cometí un error en el conteo de avena.' },
    { id: 2, type: 'Almuerzo', glucose: 145, carbs: 60, time: '12:45 PM', date: '20 Abril, 2024', note: 'Comida en restaurante, dosis corregida.' },
    { id: 3, type: 'Merienda', glucose: 105, carbs: 15, time: '04:15 PM', date: '20 Abril, 2024' },
    { id: 4, type: 'Cena', glucose: 125, carbs: 50, time: '08:45 PM', date: '19 Abril, 2024', note: 'Sentía fatiga moderada.' },
    { id: 5, type: 'Control', glucose: 95, carbs: 0, time: '07:00 AM', date: '19 Abril, 2024' },
    { id: 6, type: 'Snack', glucose: 130, carbs: 10, time: '11:20 PM', date: '18 Abril, 2024', note: 'Hambre nocturna post-entreno.' },
  ];

  constructor() {
    addIcons({ waterOutline, nutritionOutline, arrowForwardOutline, downloadOutline });
  }
}
