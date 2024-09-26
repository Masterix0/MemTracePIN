import pandas as pd
import matplotlib.pyplot as plt
import os
import glob
import math
import re

# Get the directory where the script is located
script_dir = os.path.dirname(os.path.abspath(__file__))

# Define the output directory relative to the script directory
output_dir = os.path.join(script_dir, '..', 'output')

# Define the plots directory within the visualizer directory
plots_dir = os.path.join(script_dir, 'plots')
if not os.path.exists(plots_dir):
    os.makedirs(plots_dir)

# List all CSV files in the output directory
csv_files = glob.glob(os.path.join(output_dir, '*.csv'))

# Check if any CSV files are found
if not csv_files:
    print(f"No CSV files found in {output_dir}.")
    exit(1)

# Initialize dictionaries to hold data and workload info
dataframes = {}
workload_info = {}

for csv_file in csv_files:
    # Extract information from the filename
    filename = os.path.basename(csv_file)
    workload_name = os.path.splitext(filename)[0]

    # Use regex to parse the filename
    # Example filename: '603_bwaves_s-100-20.00-0.10.csv'
    pattern = r'^(.+?)-(\d+)-([\d\.]+)-([\d\.]+)$'
    match = re.match(pattern, workload_name)

    if match:
        workload_base_name = match.group(1)
        scan_interval_duration_ms = float(match.group(2))
        sub_interval_duration_ms = float(match.group(3))
        simulated_dram_capacity_percent = float(match.group(4)) * 100  # Convert to percentage
    else:
        # If the filename doesn't match the pattern, use default values
        workload_base_name = workload_name
        scan_interval_duration_ms = None
        sub_interval_duration_ms = None
        simulated_dram_capacity_percent = None

    # Read the CSV file
    df = pd.read_csv(csv_file)

    # Convert timestamps from strings to numeric values
    df['interval_start_timestamp'] = pd.to_numeric(df['interval_start_timestamp'])
    df['interval_end_timestamp'] = pd.to_numeric(df['interval_end_timestamp'])

    # Compute duration per unit (jiffy) using the first interval
    first_interval_duration_units = df['interval_end_timestamp'].iloc[0] - df['interval_start_timestamp'].iloc[0]

    if scan_interval_duration_ms is not None and first_interval_duration_units != 0:
        # Calculate duration per unit in seconds
        duration_per_unit_s = (scan_interval_duration_ms / 1000.0) / first_interval_duration_units

        # Compute timestamps in seconds relative to the first timestamp
        df['timestamp'] = (df['interval_start_timestamp'] - df['interval_start_timestamp'].iloc[0]) * duration_per_unit_s
    else:
        # If scan_interval_duration_ms is not available or division by zero occurs
        df['timestamp'] = df.index * (scan_interval_duration_ms / 1000.0 if scan_interval_duration_ms else 1.0)

    # Calculate hits for each method
    df['actual_hits'] = df['total_access_count'] * df['actual_accesses_dram_hit_ratio']
    df['estimated_hits'] = df['total_access_count'] * df['estimated_dram_hit_ratio']
    df['pts_hits'] = df['total_access_count'] * df['pts_dram_hit_ratio']

    # Store the dataframe and workload info
    dataframes[workload_name] = df
    workload_info[workload_name] = {
        'workload_base_name': workload_base_name,
        'scan_interval_duration_ms': scan_interval_duration_ms,
        'sub_interval_duration_ms': sub_interval_duration_ms,
        'simulated_dram_capacity_percent': simulated_dram_capacity_percent
    }

# Determine grid size for subplots based on the number of CSV files
num_files = len(dataframes)
cols = 3  # Set the number of columns to 3 as per your request
rows = math.ceil(num_files / cols)

# Update legends
metrics = [
    ('actual_accesses_dram_hit_ratio', 'Total Number Of Accesses Ranking', {'marker': 'o', 'linestyle': '-'}),
    ('estimated_dram_hit_ratio', 'First Access Time Ranking', {'marker': 'x', 'linestyle': '--'}),
    ('pts_dram_hit_ratio', 'Simulated PTS Scoring Ranking', {'marker': 's', 'linestyle': ':'})
]

# Function to generate subplot titles from workload info
def generate_subplot_title(workload_name):
    info = workload_info[workload_name]
    title_parts = [info['workload_base_name']]
    if info['scan_interval_duration_ms'] is not None:
        title_parts.append(f"Interval={info['scan_interval_duration_ms']}ms")
    if info['sub_interval_duration_ms'] is not None:
        title_parts.append(f"Sub-interval={info['sub_interval_duration_ms']}ms")
    if info['simulated_dram_capacity_percent'] is not None:
        title_parts.append(f"Simulated DRAM={info['simulated_dram_capacity_percent']:.0f}%")
    return ', '.join(title_parts)

# Function to create subplots with multiple metrics in each subplot
def create_subplots_multiple_metrics(metrics, ylabel, title, filename):
    fig, axes = plt.subplots(rows, cols, figsize=(18, rows * 4), sharex=False, sharey=True)
    axes = axes.flatten()
    for i, workload_name in enumerate(dataframes.keys()):
        df = dataframes[workload_name]
        ax = axes[i]
        for metric, label, style in metrics:
            ax.plot(df['timestamp'], df[metric], marker=style['marker'], linestyle=style['linestyle'], label=label)
        subplot_title = generate_subplot_title(workload_name)
        ax.set_title(subplot_title, fontsize=10)
        ax.grid(True)
        if i % cols == 0:
            ax.set_ylabel(ylabel)
        if i >= num_files - cols:
            ax.set_xlabel('Time (s)')
        ax.legend(fontsize='small')
    # Hide any unused subplots
    for j in range(i + 1, len(axes)):
        fig.delaxes(axes[j])
    fig.suptitle(title, fontsize=16)
    plt.tight_layout(rect=[0, 0.05, 1, 0.95])
    plt.savefig(os.path.join(plots_dir, filename))
    plt.close()

# Create subplots for DRAM Hit Ratios with all metrics in the same plot
create_subplots_multiple_metrics(
    metrics=metrics,
    ylabel='DRAM Hit Ratio',
    title='DRAM Hit Ratios Over Time Per Page-Ranking Mechanism',
    filename='dram_hit_ratios_collage.png'
)

# Create subplots for Number of Pages Accessed
def create_subplots_single_metric(metric, ylabel, title, filename):
    fig, axes = plt.subplots(rows, cols, figsize=(18, rows * 4), sharex=False)
    axes = axes.flatten()
    for i, workload_name in enumerate(dataframes.keys()):
        df = dataframes[workload_name]
        ax = axes[i]
        ax.plot(df['timestamp'], df[metric], marker='o', linestyle='-')
        subplot_title = generate_subplot_title(workload_name)
        ax.set_title(subplot_title, fontsize=10)
        ax.grid(True)
        if i % cols == 0:
            ax.set_ylabel(ylabel)
        if i >= num_files - cols:
            ax.set_xlabel('Time (s)')
    # Hide any unused subplots
    for j in range(i + 1, len(axes)):
        fig.delaxes(axes[j])
    fig.suptitle(title, fontsize=16)
    plt.tight_layout(rect=[0, 0.05, 1, 0.95])
    plt.savefig(os.path.join(plots_dir, filename))
    plt.close()

# Plot Number of Pages Accessed
create_subplots_single_metric(
    metric='number_of_pages_accessed',
    ylabel='Number of Pages Accessed',
    title='Number of Pages Accessed Over Time',
    filename='pages_accessed_collage.png'
)

# Plot Total Access Count
create_subplots_single_metric(
    metric='total_access_count',
    ylabel='Total Access Count',
    title='Total Access Count Over Time',
    filename='total_access_count_collage.png'
)

# Plot Difference Between Actual and Estimated DRAM Hit Ratios
def create_subplots_hit_ratio_difference(ylabel, title, filename):
    fig, axes = plt.subplots(rows, cols, figsize=(18, rows * 4), sharex=False, sharey=True)
    axes = axes.flatten()
    for i, workload_name in enumerate(dataframes.keys()):
        df = dataframes[workload_name]
        ax = axes[i]
        df['Hit Ratio Difference'] = df['actual_accesses_dram_hit_ratio'] - df['estimated_dram_hit_ratio']
        ax.plot(df['timestamp'], df['Hit Ratio Difference'], marker='o', linestyle='-')
        # Indicate negative differences
        ax.fill_between(df['timestamp'], df['Hit Ratio Difference'], where=(df['Hit Ratio Difference'] < 0), color='red', alpha=0.3)
        subplot_title = generate_subplot_title(workload_name)
        ax.set_title(subplot_title, fontsize=10)
        ax.grid(True)
        if i % cols == 0:
            ax.set_ylabel(ylabel)
        if i >= num_files - cols:
            ax.set_xlabel('Time (s)')
    # Hide any unused subplots
    for j in range(i + 1, len(axes)):
        fig.delaxes(axes[j])
    fig.suptitle(title, fontsize=14)
    plt.tight_layout(rect=[0, 0.05, 1, 0.93])
    plt.savefig(os.path.join(plots_dir, filename))
    plt.close()

create_subplots_hit_ratio_difference(
    ylabel='Difference in DRAM Hit Ratio',
    title='Difference Between Total Number Of Accesses Ranking and First Access Time Ranking Over Time',
    filename='hit_ratio_difference_collage.png'
)

# Create a collage of bar charts for overall DRAM hit ratios per workload
def create_bar_charts_collage(filename):
    fig, axes = plt.subplots(rows, cols, figsize=(18, rows * 4))
    axes = axes.flatten()
    for i, workload_name in enumerate(dataframes.keys()):
        df = dataframes[workload_name]
        ax = axes[i]

        # Ensure 'actual_hits' and other hit columns exist
        if 'actual_hits' not in df.columns:
            # Calculate hits for each method
            df['actual_hits'] = df['total_access_count'] * df['actual_accesses_dram_hit_ratio']
            df['estimated_hits'] = df['total_access_count'] * df['estimated_dram_hit_ratio']
            df['pts_hits'] = df['total_access_count'] * df['pts_dram_hit_ratio']

        # Calculate totals for this workload
        total_accesses = df['total_access_count'].sum()
        total_actual_hits = df['actual_hits'].sum()
        total_estimated_hits = df['estimated_hits'].sum()
        total_pts_hits = df['pts_hits'].sum()

        # Calculate hit ratios
        actual_ratio = total_actual_hits / total_accesses
        estimated_ratio = total_estimated_hits / total_accesses
        pts_ratio = total_pts_hits / total_accesses

        # Prepare data for the bar chart
        methods = ['Total Number\nOf Accesses', 'First Access\nTime', 'Simulated PTS\nScoring']
        hit_ratios = [actual_ratio, estimated_ratio, pts_ratio]
        colors = ['blue', 'orange', 'green']

        # Create the bar chart
        bars = ax.bar(methods, hit_ratios, color=colors)
        ax.set_ylim(0, 1)
        subplot_title = generate_subplot_title(workload_name)
        ax.set_title(subplot_title, fontsize=10)
        ax.set_ylabel('DRAM Hit Ratio')
        ax.grid(axis='y')

        # Annotate bars with values
        for bar, ratio in zip(bars, hit_ratios):
            yval = bar.get_height()
            ax.text(bar.get_x() + bar.get_width()/2.0, yval + 0.02, f'{ratio:.3f}', ha='center', va='bottom', fontsize=8)

        # Adjust x-axis labels
        ax.set_xticklabels(methods, rotation=0, fontsize=8)
    
    # Hide any unused subplots
    for j in range(i + 1, len(axes)):
        fig.delaxes(axes[j])

    fig.suptitle('Overall DRAM Hit Ratios Per Workload', fontsize=16)
    plt.tight_layout(rect=[0, 0.05, 1, 0.93])
    plt.savefig(os.path.join(plots_dir, filename))
    plt.close()

# Call the function to create the collage of bar charts
create_bar_charts_collage(filename='overall_dram_hit_ratios_collage.png')
