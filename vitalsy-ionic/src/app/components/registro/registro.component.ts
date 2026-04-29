import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule, NavController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { nutritionOutline, chevronBackOutline, waterOutline } from 'ionicons/icons';

import { HeaderComponent } from '../header/header.component';

@Component({
  selector: 'app-registro',
  templateUrl: './registro.component.html',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, HeaderComponent]
})
export class RegistroComponent {
  glucose: string = '';
  carbs: string = '';
  date: string = new Date().toISOString().slice(0, 16);
  selectedMoment: string = 'Ayunas';
  notes: string = '';

  moments: string[] = [
    'Ayunas', 'Pre-Desayuno', 'Post-Desayuno', 
    'Pre-Almuerzo', 'Post-Almuerzo', 'Merienda', 
    'Pre-Cena', 'Post-Cena', 'Ejercicio', 'Antes de Dormir'
  ];

  constructor(private navCtrl: NavController) {
    addIcons({ nutritionOutline, chevronBackOutline, waterOutline });
  }

  selectMoment(moment: string) {
    this.selectedMoment = moment;
  }

  saveRecord() {
    console.log('Record saved', { glucose: this.glucose, carbs: this.carbs, date: this.date, moment: this.selectedMoment, notes: this.notes });
    this.navCtrl.navigateBack('/dashboard');
  }
}
